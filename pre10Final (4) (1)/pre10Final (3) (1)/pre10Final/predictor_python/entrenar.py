
import json
import unicodedata
from sklearn.neural_network import MLPClassifier
from pathlib import Path

import joblib
import numpy as np
import pandas as pd
from scipy.io import arff

from sklearn.compose import ColumnTransformer
from sklearn.ensemble import RandomForestClassifier
from sklearn.impute import SimpleImputer
from sklearn.linear_model import LogisticRegression
from sklearn.metrics import (
    accuracy_score,
    f1_score,
    classification_report,
    confusion_matrix
)
from sklearn.model_selection import train_test_split
from sklearn.pipeline import Pipeline
from sklearn.preprocessing import OneHotEncoder, StandardScaler


BASE = Path(__file__).resolve().parent

ARCHIVO = BASE / "datos" / "plan_nutricional_360_balanceado.arff"
MODELO = BASE / "modelos" / "modelo_adherencia.joblib"
REPORTE = BASE / "modelos" / "evaluacion.json"

COLUMNAS = [
    "edad",
    "genero",
    "motivacion_inicial",
    "intentos_previos",
    "sistema_apoyo",
    "actividad_fisica",
    "complejidad_plan",
    "estres",
    "afinidad_plan"
]

NUMERICAS = ["edad", "intentos_previos"]
CATEGORICAS = [c for c in COLUMNAS if c not in NUMERICAS]


def normalizar(valor):
    if pd.isna(valor):
        return np.nan

    if isinstance(valor, bytes):
        try:
            valor = valor.decode("utf-8")
        except UnicodeDecodeError:
            valor = valor.decode("latin-1")

    texto = str(valor).strip()

    if texto in ("", "?"):
        return np.nan

    texto = "".join(
        c for c in unicodedata.normalize("NFKD", texto)
        if not unicodedata.combining(c)
    )

    return texto.lower()


def cargar_datos():
    if not ARCHIVO.exists():
        raise FileNotFoundError(
            f"No se encontró el dataset: {ARCHIVO}"
        )

    datos, _ = arff.loadarff(str(ARCHIVO))
    df = pd.DataFrame(datos)

    faltantes = [c for c in COLUMNAS if c not in df.columns]

    if faltantes:
        raise ValueError(
            f"Faltan columnas en el ARFF: {faltantes}"
        )

    # Igual que en el proyecto WEKA:
    # la última columna representa la clase.
    objetivo = df.columns[-1]

    if objetivo in COLUMNAS:
        raise ValueError(
            "La última columna debe ser la clase de adherencia."
        )

    for columna in CATEGORICAS:
        df[columna] = df[columna].map(normalizar)

    for columna in NUMERICAS:
        df[columna] = pd.to_numeric(
            df[columna], errors="coerce"
        )

    df[objetivo] = df[objetivo].map(normalizar)
    df = df.dropna(subset=[objetivo])

    if df.empty:
        raise ValueError("El dataset no tiene etiquetas válidas.")

    return df, objetivo


def crear_pipeline(algoritmo):
    preparacion = ColumnTransformer([
        (
            "numericas",
            Pipeline([
                ("imputar", SimpleImputer(strategy="median")),
                ("escalar", StandardScaler())
            ]),
            NUMERICAS
        ),
        (
            "categoricas",
            Pipeline([
                ("imputar", SimpleImputer(strategy="most_frequent")),
                ("codificar", OneHotEncoder(handle_unknown="ignore"))
            ]),
            CATEGORICAS
        )
    ])

    return Pipeline([
        ("preparacion", preparacion),
        ("modelo", algoritmo)
    ])


def entrenar():
    df, objetivo = cargar_datos()

    X = df[COLUMNAS]
    y = df[objetivo]

    conteos = y.value_counts()

    if len(conteos) < 2 or conteos.min() < 2:
        raise ValueError(
            "Se necesitan al menos dos clases y dos "
            "registros por clase."
        )

    X_train, X_test, y_train, y_test = train_test_split(
        X,
        y,
        test_size=0.20,
        random_state=42,
        stratify=y
    )

    algoritmos = {
        "Random Forest": RandomForestClassifier(
            n_estimators=250,
            random_state=42,
            class_weight="balanced",
            n_jobs=-1
        ),
        "Regresion Logistica": LogisticRegression(
            max_iter=2000,
            class_weight="balanced",
            random_state=42
        ),
        "Red Neuronal MLP": MLPClassifier(
            hidden_layer_sizes=(32, 16),
            activation="relu",
            solver="adam",
            max_iter=1500,
            early_stopping=True,
            random_state=42
)
        
    }

    resultados = {}
    modelos_entrenados = {}

    for nombre, algoritmo in algoritmos.items():
        pipeline = crear_pipeline(algoritmo)

        pipeline.fit(X_train, y_train)
        predicciones = pipeline.predict(X_test)

        f1 = f1_score(
            y_test,
            predicciones,
            average="macro",
            zero_division=0
        )

        accuracy = accuracy_score(y_test, predicciones)

        resultados[nombre] = {
            "f1_macro": round(float(f1), 4),
            "accuracy": round(float(accuracy), 4),
            "matriz_confusion": confusion_matrix(
                y_test,
                predicciones,
                labels=pipeline.classes_
            ).tolist(),
            "reporte": classification_report(
                y_test,
                predicciones,
                labels=pipeline.classes_,
                output_dict=True,
                zero_division=0
            )
        }

        modelos_entrenados[nombre] = pipeline

        print(f"{nombre}")
        print(f"  Accuracy: {accuracy:.4f}")
        print(f"  F1 macro: {f1:.4f}")

    mejor = max(
        resultados,
        key=lambda nombre: resultados[nombre]["f1_macro"]
    )

    MODELO.parent.mkdir(parents=True, exist_ok=True)

    joblib.dump({
        "pipeline": modelos_entrenados[mejor],
        "columnas": COLUMNAS,
        "algoritmo": mejor
    }, MODELO)

    resumen = {
        "total_registros": len(df),
        "entrenamiento": len(X_train),
        "prueba": len(X_test),
        "variable_objetivo": objetivo,
        "distribucion_clases": {
            str(k): int(v) for k, v in conteos.items()
        },
        "modelo_seleccionado": mejor,
        "evaluacion": resultados
    }

    REPORTE.write_text(
        json.dumps(resumen, indent=2, ensure_ascii=False),
        encoding="utf-8"
    )

    print("\nEntrenamiento finalizado.")
    print(f"Modelo seleccionado: {mejor}")
    print(f"Modelo guardado en: {MODELO}")


if __name__ == "__main__":
    entrenar()
