
import json
import unicodedata
import warnings
from pathlib import Path

import joblib
import numpy as np
import pandas as pd
from scipy.io import arff

from sklearn.compose import ColumnTransformer
from sklearn.ensemble import RandomForestClassifier
from sklearn.exceptions import ConvergenceWarning
from sklearn.impute import SimpleImputer
from sklearn.linear_model import LogisticRegression
from sklearn.metrics import (
    accuracy_score,
    f1_score,
    classification_report,
    confusion_matrix
)
from sklearn.model_selection import train_test_split
from sklearn.neural_network import MLPClassifier
from sklearn.pipeline import Pipeline
from sklearn.preprocessing import OneHotEncoder, StandardScaler


# ==========================================
# NUTRI VIDA - CONFIGURACION
# ==========================================

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

NUMERICAS = [
    "edad",
    "intentos_previos"
]

CATEGORICAS = [
    columna
    for columna in COLUMNAS
    if columna not in NUMERICAS
]

# Modelo que utilizara FastAPI
MODELO_OBLIGATORIO = "Red Neuronal MLP"

# Maximo de epocas para la red neuronal
MAX_ITERACIONES = 500


# ==========================================
# NORMALIZACION DE DATOS
# ==========================================

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
        caracter
        for caracter in unicodedata.normalize("NFKD", texto)
        if not unicodedata.combining(caracter)
    )

    return texto.lower()


# ==========================================
# CARGAR DATASET ARFF
# ==========================================

def cargar_datos():

    if not ARCHIVO.exists():
        raise FileNotFoundError(
            f"No se encontro el dataset: {ARCHIVO}"
        )

    datos, _ = arff.loadarff(str(ARCHIVO))

    df = pd.DataFrame(datos)

    faltantes = [
        columna
        for columna in COLUMNAS
        if columna not in df.columns
    ]

    if faltantes:
        raise ValueError(
            f"Faltan columnas en el ARFF: {faltantes}"
        )

    # La ultima columna corresponde a la clase
    objetivo = df.columns[-1]

    if objetivo in COLUMNAS:
        raise ValueError(
            "La ultima columna debe ser la clase de adherencia."
        )

    for columna in CATEGORICAS:
        df[columna] = df[columna].map(normalizar)

    for columna in NUMERICAS:
        df[columna] = pd.to_numeric(
            df[columna],
            errors="coerce"
        )

    df[objetivo] = df[objetivo].map(normalizar)

    df = df.dropna(subset=[objetivo])

    if df.empty:
        raise ValueError(
            "El dataset no tiene etiquetas validas."
        )

    return df, objetivo


# ==========================================
# PREPROCESAMIENTO
# ==========================================

def crear_pipeline(algoritmo):

    preparacion = ColumnTransformer([
        (
            "numericas",
            Pipeline([
                (
                    "imputar",
                    SimpleImputer(strategy="median")
                ),
                (
                    "escalar",
                    StandardScaler()
                )
            ]),
            NUMERICAS
        ),
        (
            "categoricas",
            Pipeline([
                (
                    "imputar",
                    SimpleImputer(
                        strategy="most_frequent"
                    )
                ),
                (
                    "codificar",
                    OneHotEncoder(
                        handle_unknown="ignore",
                        sparse_output=False
                    )
                )
            ]),
            CATEGORICAS
        )
    ])

    return Pipeline([
        ("preparacion", preparacion),
        ("modelo", algoritmo)
    ])


# ==========================================
# ENTRENAMIENTO DE MODELOS
# ==========================================

def entrenar():

    print("\n========================================")
    print("NUTRI VIDA - ENTRENAMIENTO PREDICTIVO")
    print("========================================")

    df, objetivo = cargar_datos()

    X = df[COLUMNAS]
    y = df[objetivo]

    conteos = y.value_counts()

    if len(conteos) < 2 or conteos.min() < 2:
        raise ValueError(
            "Se necesitan al menos dos clases "
            "y dos registros por clase."
        )

    print(f"\nTotal de registros: {len(df)}")
    print(f"Variable objetivo: {objetivo}")

    # Separar entrenamiento y prueba
    X_train, X_test, y_train, y_test = train_test_split(
        X,
        y,
        test_size=0.20,
        random_state=42,
        stratify=y
    )

    print(f"Registros de entrenamiento: {len(X_train)}")
    print(f"Registros de prueba: {len(X_test)}")

    # ======================================
    # ALGORITMOS A COMPARAR
    # ======================================

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
            learning_rate_init=0.001,
            alpha=0.01,
            max_iter=MAX_ITERACIONES,
            early_stopping=True,
            validation_fraction=0.15,
            n_iter_no_change=30,
            random_state=42
        )
    }

    resultados = {}
    modelos_entrenados = {}

    # ======================================
    # ENTRENAR Y EVALUAR
    # ======================================

    for nombre, algoritmo in algoritmos.items():

        print("\n----------------------------------------")
        print(f"Entrenando: {nombre}")
        print("----------------------------------------")

        pipeline = crear_pipeline(algoritmo)

        with warnings.catch_warnings(record=True) as avisos:
            warnings.simplefilter(
                "always",
                ConvergenceWarning
            )
            pipeline.fit(X_train, y_train)

        for aviso in avisos:
            if issubclass(
                aviso.category,
                ConvergenceWarning
            ):
                print(
                    "Aviso de convergencia:",
                    str(aviso.message)
                )

        predicciones = pipeline.predict(X_test)

        accuracy = accuracy_score(
            y_test,
            predicciones
        )

        f1 = f1_score(
            y_test,
            predicciones,
            average="macro",
            zero_division=0
        )

        resultados[nombre] = {
            "accuracy": round(float(accuracy), 4),
            "f1_macro": round(float(f1), 4),
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

        print(f"Accuracy: {accuracy:.4f}")
        print(f"F1 macro: {f1:.4f}")

        # Mostrar informacion de entrenamiento MLP
        if nombre == MODELO_OBLIGATORIO:

            red = pipeline.named_steps["modelo"]

            resultados[nombre]["iteraciones"] = int(
                red.n_iter_
            )

            resultados[nombre]["max_iteraciones"] = (
                MAX_ITERACIONES
            )

            resultados[nombre]["arquitectura"] = list(
                red.hidden_layer_sizes
            )

            resultados[nombre]["perdida_final"] = float(
                red.loss_
            )

            print("\nDATOS DE LA RED NEURONAL:")
            print("Tipo: MLPClassifier")
            print("Capas ocultas: (32, 16)")
            print(
                f"Iteraciones realizadas: "
                f"{red.n_iter_}/{MAX_ITERACIONES}"
            )
            print(
                f"Perdida final: {red.loss_:.6f}"
            )

    # ======================================
    # SELECCIONAR LA RED NEURONAL
    # ======================================

    # Conservar la comparacion de resultados,
    # pero NO seleccionar Random Forest.

    mejor_por_f1 = max(
        resultados,
        key=lambda nombre: resultados[nombre]["f1_macro"]
    )

    elegido = MODELO_OBLIGATORIO

    print("\n========================================")
    print("COMPARACION FINAL DE MODELOS")
    print("========================================")

    for nombre, metricas in resultados.items():

        print(
            f"{nombre}: "
            f"Accuracy={metricas['accuracy']:.4f} | "
            f"F1={metricas['f1_macro']:.4f}"
        )

    print(
        f"\nMejor algoritmo segun F1: {mejor_por_f1}"
    )

    print(
        f"Modelo seleccionado para NUTRI VIDA: {elegido}"
    )

    # ======================================
    # GUARDAR MODELO NEURONAL
    # ======================================

    MODELO.parent.mkdir(
        parents=True,
        exist_ok=True
    )

    joblib.dump(
        {
            "pipeline": modelos_entrenados[elegido],
            "columnas": COLUMNAS,
            "algoritmo": elegido
        },
        MODELO
    )

    # ======================================
    # GENERAR REPORTE DE EVALUACION
    # ======================================

    red_entrenada = modelos_entrenados[
        elegido
    ].named_steps["modelo"]

    resumen = {
        "total_registros": int(len(df)),
        "entrenamiento": int(len(X_train)),
        "prueba": int(len(X_test)),
        "variable_objetivo": str(objetivo),
        "distribucion_clases": {
            str(clase): int(cantidad)
            for clase, cantidad in conteos.items()
        },
        "modelo_seleccionado": elegido,
        "mejor_modelo_segun_f1": mejor_por_f1,
        "tipo_modelo": "Red Neuronal Artificial MLP",
        "arquitectura": [32, 16],
        "iteraciones_maximas": MAX_ITERACIONES,
        "iteraciones_realizadas": int(
            red_entrenada.n_iter_
        ),
        "evaluacion": resultados
    }

    REPORTE.write_text(
        json.dumps(
            resumen,
            indent=2,
            ensure_ascii=False
        ),
        encoding="utf-8"
    )

    # ======================================
    # RESULTADO FINAL
    # ======================================

    print("\n========================================")
    print("ENTRENAMIENTO FINALIZADO")
    print("========================================")

    print("Modelo seleccionado: Red Neuronal MLP")

    print(
        f"Iteraciones realizadas: "
        f"{red_entrenada.n_iter_}/{MAX_ITERACIONES}"
    )

    print(f"Modelo guardado en: {MODELO}")
    print(f"Evaluacion guardada en: {REPORTE}")

    print("========================================\n")


if __name__ == "__main__":
    entrenar()
