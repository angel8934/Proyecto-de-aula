
import json

from functools import lru_cache
from pathlib import Path
from typing import Literal

import joblib
import pandas as pd
import plotly.graph_objects as go

from fastapi import FastAPI, HTTPException
from pydantic import BaseModel, Field


MODELO = (
    Path(__file__).resolve().parent
    / "modelos"
    / "modelo_adherencia.joblib"
)

app = FastAPI(
    title="NUTRI VIDA - Predictor de adherencia",
    version="2.0.0"
)


class Paciente(BaseModel):

    # Campos anteriores, temporalmente opcionales
    age: int | None = Field(
        default=None,
        ge=18,
        le=100
    )

    gender: Literal[
        "male", "female"
    ] | None = None

    activityLevel: Literal[
        "high", "moderate", "medium", "low"
    ] | None = None

    # Seis variables predictoras
    motivation: Literal[
        "high", "medium", "low"
    ]

    previousDiets: int = Field(
        ge=0,
        le=3
    )

    supportSystem: Literal[
        "strong", "moderate", "weak"
    ]

    dietComplexity: Literal[
        "simple", "moderate", "medium", "complex"
    ]

    stressLevel: Literal[
        "high", "moderate", "medium", "low"
    ]

    foodPreferences: Literal[
        "high", "moderate", "medium", "low"
    ]


@lru_cache(maxsize=1)
def cargar_modelo():

    if not MODELO.is_file():
        raise FileNotFoundError(
            f"Falta el modelo: {MODELO}"
        )

    return joblib.load(MODELO)


def convertir_a_columnas(paciente):

    p = paciente.model_dump()

    return {
        "motivacion_inicial": {
            "high": "alto",
            "medium": "medio",
            "low": "bajo"
        }[p["motivation"]],

        "intentos_previos": p["previousDiets"],

        "sistema_apoyo": {
            "strong": "fuerte",
            "moderate": "moderado",
            "weak": "debil"
        }[p["supportSystem"]],

        "complejidad_plan": {
            "simple": "simple",
            "moderate": "moderada",
            "medium": "moderada",
            "complex": "compleja"
        }[p["dietComplexity"]],

        "estres": {
            "high": "alto",
            "moderate": "moderado",
            "medium": "moderado",
            "low": "bajo"
        }[p["stressLevel"]],

        "afinidad_plan": {
            "high": "alta",
            "moderate": "moderada",
            "medium": "moderada",
            "low": "baja"
        }[p["foodPreferences"]]
    }


def radar_descriptivo(paciente):

    p = paciente.model_dump()

    niveles = {
        "high": 100,
        "medium": 60,
        "moderate": 60,
        "low": 20,
        "strong": 100,
        "weak": 20,
        "simple": 100,
        "complex": 20
    }

    return {
        "categorias": [
            "Motivacion",
            "Sistema de apoyo",
            "Facilidad del plan",
            "Manejo del estres",
            "Afinidad con el plan",
            "Intentos previos"
        ],

        "valores": [
            niveles[p["motivation"]],
            niveles[p["supportSystem"]],
            niveles[p["dietComplexity"]],

            {
                "low": 100,
                "medium": 60,
                "moderate": 60,
                "high": 20
            }[p["stressLevel"]],

            niveles[p["foodPreferences"]],

            round(
                100 - p["previousDiets"] * (80 / 3),
                1
            )
        ],

        "nota": (
            "Radar descriptivo sintetico; "
            "no representa un porcentaje "
            "de adherencia."
        )
    }


def nombre_clase(valor, artefacto):

    etiquetas = artefacto.get("etiquetas")

    if etiquetas is not None:
        return str(etiquetas[int(valor)])

    return str(valor)


@app.get("/salud")
def salud():

    return {
        "estado": "activo",
        "modelo_entrenado": MODELO.is_file(),
        "version": "v2-6-variables"
    }


@app.post("/predecir")
def predecir(paciente: Paciente):

    try:
        artefacto = cargar_modelo()
    except FileNotFoundError as exc:
        raise HTTPException(
            status_code=503,
            detail=str(exc)
        ) from exc

    pipeline = artefacto["pipeline"]
    columnas = artefacto["columnas"]

    entrada = pd.DataFrame([
        convertir_a_columnas(paciente)
    ])[columnas]

    prediccion = pipeline.predict(entrada)[0]

    clase = nombre_clase(
        prediccion,
        artefacto
    )

    probabilidades = pipeline.predict_proba(
        entrada
    )[0]

    mapa = {
        nombre_clase(valor, artefacto): round(
            float(probabilidad) * 100,
            2
        )
        for valor, probabilidad
        in zip(
            pipeline.classes_,
            probabilidades
        )
    }

    return {
        "nivel": clase,
    "porcentaje": mapa[clase],
    "descripcion": (
        f"El modelo MLP estima adherencia {clase} "
        "según los seis factores evaluados. "
        "Resultado experimental basado en datos entrenados por nuestro modelo predictivo."
    ),
        "tipo_porcentaje": (
            "Probabilidad estimada de la "
            "clase predicha; no es el "
            "porcentaje de cumplimiento "
            "del plan."
        ),
        "probabilidades": mapa,
        "radar": radar_descriptivo(paciente),
        "aviso": (
            "Prediccion academica basada "
            "en datos , no "
            "validada clinicamente."
        )
    }


@app.post("/radar")
def radar(paciente: Paciente):

    datos = radar_descriptivo(paciente)

    figura = go.Figure()

    figura.add_trace(
        go.Scatterpolar(
            r=datos["valores"],
            theta=datos["categorias"],
            fill="toself",
            name="Perfil del paciente"
        )
    )

    figura.update_layout(
        polar={
            "radialaxis": {
                "visible": True,
                "range": [0, 100]
            }
        },
        showlegend=False
    )

    return json.loads(figura.to_json())
