
from functools import lru_cache
from pathlib import Path
from typing import Literal

import joblib
import pandas as pd

from fastapi import FastAPI, HTTPException
from pydantic import BaseModel, Field

from radar import datos_radar, crear_radar


app = FastAPI(
    title="Sistema Predictivo Nutricional",
    description="Predicción de adherencia a planes nutricionales",
    version="1.0.0"
)

BASE = Path(__file__).resolve().parent
RUTA_MODELO = BASE / "modelos" / "modelo_adherencia.joblib"


class Paciente(BaseModel):
    age: int = Field(ge=18, le=100)

    gender: Literal["male", "female"]

    motivation: Literal["high", "medium", "low"]

    previousDiets: int = Field(ge=0, le=20)

    supportSystem: Literal[
        "strong", "moderate", "weak"
    ]

    activityLevel: Literal[
        "high", "moderate", "low"
    ]

    stressLevel: Literal[
        "high", "moderate", "low"
    ]

    dietComplexity: Literal[
        "simple", "moderate", "complex"
    ]

    foodPreferences: Literal[
        "high", "moderate", "low"
    ]


@lru_cache(maxsize=1)
def cargar_modelo():
    if not RUTA_MODELO.exists():
        raise FileNotFoundError(
            "Modelo no encontrado. Ejecuta entrenar.py."
        )

    return joblib.load(RUTA_MODELO)


def preparar_paciente(paciente: Paciente):
    return {
        "edad": paciente.age,

        "genero": {
            "male": "masculino",
            "female": "femenino"
        }[paciente.gender],

        "motivacion_inicial": {
            "high": "alto",
            "medium": "medio",
            "low": "bajo"
        }[paciente.motivation],

        "intentos_previos": paciente.previousDiets,

        "sistema_apoyo": {
            "strong": "fuerte",
            "moderate": "moderado",
            "weak": "debil"
        }[paciente.supportSystem],

        "actividad_fisica": {
            "high": "alto",
            "moderate": "moderado",
            "low": "bajo"
        }[paciente.activityLevel],

        "complejidad_plan": {
            "simple": "simple",
            "moderate": "moderada",
            "complex": "compleja"
        }[paciente.dietComplexity],

        "estres": {
            "high": "alto",
            "moderate": "moderado",
            "low": "bajo"
        }[paciente.stressLevel],

        "afinidad_plan": {
            "high": "alta",
            "moderate": "moderada",
            "low": "baja"
        }[paciente.foodPreferences]
    }


@app.get("/")
def inicio():
    return {
        "mensaje": "API predictiva nutricional activa"
    }


@app.get("/salud")
def verificar_salud():
    return {
        "estado": "activo",
        "modelo_entrenado": RUTA_MODELO.exists()
    }


@app.post("/predecir")
def predecir(paciente: Paciente):
    try:
        artefacto = cargar_modelo()

    except FileNotFoundError as error:
        raise HTTPException(
            status_code=503,
            detail=str(error)
        )

    pipeline = artefacto["pipeline"]

    datos = preparar_paciente(paciente)

    df = pd.DataFrame([datos])
    df = df[artefacto["columnas"]]

    prediccion = pipeline.predict(df)[0]

    probabilidades = pipeline.predict_proba(df)[0]

    clases = pipeline.classes_

    porcentajes = {
        str(clase): round(float(prob) * 100, 2)
        for clase, prob in zip(clases, probabilidades)
    }

    nivel = str(prediccion)

    return {
        "nivel": nivel,
        "porcentaje": porcentajes[nivel],
        "probabilidades": porcentajes,
        "radar": datos_radar(paciente.model_dump()),
        "descripcion": (
            f"Nivel de adherencia estimado: {nivel}"
        ),
        "aviso": (
            "Predicción académica. "
            "Requiere validación antes de uso clínico."
        )
    }


@app.post("/radar")
def obtener_radar(paciente: Paciente):
    return crear_radar(paciente.model_dump())
