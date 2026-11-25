
import json
import unicodedata
from pathlib import Path

import joblib
import numpy as np
import pandas as pd

from scipy.io import arff
from sklearn.compose import ColumnTransformer
from sklearn.impute import SimpleImputer
from sklearn.model_selection import (
    StratifiedGroupKFold,
    cross_validate
)
from sklearn.neural_network import MLPClassifier
from sklearn.pipeline import Pipeline
from sklearn.preprocessing import (
    OneHotEncoder,
    StandardScaler,
    LabelEncoder
)


BASE = Path(__file__).resolve().parent

ARFF = (
    BASE / "datos"
    / "plan_nutricional_360_sintetico_v2.arff"
)

MODELO = (
    BASE / "modelos"
    / "modelo_adherencia.joblib"
)

REPORTE = (
    BASE / "modelos"
    / "evaluacion.json"
)

# Las seis variables definitivas
COLUMNAS = [
    "motivacion_inicial",
    "intentos_previos",
    "sistema_apoyo",
    "complejidad_plan",
    "estres",
    "afinidad_plan"
]

NUMERICAS = ["intentos_previos"]

CATEGORICAS = [
    c for c in COLUMNAS
    if c not in NUMERICAS
]

GRUPOS = [
    "motivacion_inicial",
    "intentos_previos",
    "sistema_apoyo",
    "complejidad_plan",
    "estres"
]


def normalizar(valor):

    if pd.isna(valor):
        return np.nan

    if isinstance(valor, bytes):
        valor = valor.decode("utf-8")

    texto = str(valor).strip()

    if texto in ("", "?"):
        return np.nan

    texto = "".join(
        c for c in unicodedata.normalize(
            "NFKD", texto
        )
        if not unicodedata.combining(c)
    )

    return texto.lower()


def cargar_datos():

    if not ARFF.exists():
        raise FileNotFoundError(ARFF)

    datos, _ = arff.loadarff(str(ARFF))
    df = pd.DataFrame(datos)

    faltantes = [
        col for col in COLUMNAS + ["clase"]
        if col not in df.columns
    ]

    if faltantes:
        raise ValueError(
            f"Faltan columnas: {faltantes}"
        )

    for col in CATEGORICAS + ["clase"]:
        df[col] = df[col].map(normalizar)

    df["intentos_previos"] = pd.to_numeric(
        df["intentos_previos"],
        errors="coerce"
    )

    if df[COLUMNAS + ["clase"]].isna().any().any():
        raise ValueError(
            "Hay valores faltantes en el ARFF"
        )

    return df, "clase"


def crear_modelo():

    preprocesamiento = ColumnTransformer([
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

    mlp = MLPClassifier(
        hidden_layer_sizes=(32, 16),
        activation="relu",
        solver="adam",
        learning_rate_init=0.001,
        alpha=0.01,
        max_iter=500,
        early_stopping=True,
        validation_fraction=0.15,
        n_iter_no_change=30,
        random_state=42
    )

    return Pipeline([
        ("preparacion", preprocesamiento),
        ("modelo", mlp)
    ])


def entrenar():

    df, etiqueta = cargar_datos()

    X = df[COLUMNAS]

    # Codificacion interna de las etiquetas.
    # Al responder desde FastAPI volveremos
    # a usar los nombres alta, media y baja.
    encoder = LabelEncoder()
    y = encoder.fit_transform(df[etiqueta])

    grupos = (
        df.groupby(GRUPOS, dropna=False)
        .ngroup()
        .to_numpy()
    )

    cv = StratifiedGroupKFold(
        n_splits=5,
        shuffle=True,
        random_state=42
    )

    resultados = cross_validate(
        crear_modelo(),
        X,
        y,
        groups=grupos,
        cv=cv,
        scoring={
            "accuracy": "accuracy",
            "f1_macro": "f1_macro"
        },
        error_score="raise",
        n_jobs=1
    )

    exactitud = float(
        np.mean(resultados["test_accuracy"])
    )

    f1 = float(
        np.mean(resultados["test_f1_macro"])
    )

    print("\nNUTRI VIDA - MODELO MLP V2")
    print("Registros:", len(df))
    print("Variables:", len(COLUMNAS))
    print(f"Accuracy CV: {exactitud:.4f}")
    print(f"F1 macro CV: {f1:.4f}")

    # Entrenar el modelo que utilizara la API.
    modelo = crear_modelo()
    modelo.fit(X, y)

    MODELO.parent.mkdir(
        parents=True,
        exist_ok=True
    )

    joblib.dump({
        "pipeline": modelo,
        "columnas": COLUMNAS,
        "algoritmo": "Red Neuronal MLP",
        "dataset": ARFF.name,
        "tipo_datos": "sinteticos",
        "version": "v2-6-variables",
        "etiquetas": encoder.classes_.tolist()
    }, MODELO)

    REPORTE.write_text(
        json.dumps({
            "dataset": ARFF.name,
            "registros": len(df),
            "columnas": COLUMNAS,
            "accuracy_cv": exactitud,
            "f1_macro_cv": f1,
            "f1_por_particion": [
                float(x)
                for x in resultados["test_f1_macro"]
            ],
            "advertencia": (
                "Datos sinteticos, "
                "sin validacion clinica."
            )
        }, ensure_ascii=False, indent=2),
        encoding="utf-8"
    )

    print("\nModelo guardado:")
    print(MODELO)


if __name__ == "__main__":
    entrenar()
