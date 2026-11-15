
import json
import plotly.graph_objects as go


PUNTUACIONES = {
    "high": 100,
    "medium": 60,
    "moderate": 60,
    "low": 20,
    "strong": 100,
    "weak": 20
}


def datos_radar(paciente):
    categorias = [
        "Motivación",
        "Sistema de apoyo",
        "Actividad física",
        "Manejo del estrés",
        "Afinidad con el plan"
    ]

    valores = [
        PUNTUACIONES[paciente["motivation"]],
        PUNTUACIONES[paciente["supportSystem"]],
        PUNTUACIONES[paciente["activityLevel"]],
        {
            "low": 100,
            "moderate": 60,
            "medium": 60,
            "high": 20
        }[paciente["stressLevel"]],
        PUNTUACIONES[paciente["foodPreferences"]]
    ]

    return {
        "categorias": categorias,
        "valores": valores,
        "nota": (
            "Escala descriptiva del perfil del paciente, "
            "no una probabilidad clínica."
        )
    }


def crear_radar(paciente):
    datos = datos_radar(paciente)

    fig = go.Figure()

    fig.add_trace(
        go.Scatterpolar(
            r=datos["valores"],
            theta=datos["categorias"],
            fill="toself",
            name="Paciente"
        )
    )

    fig.update_layout(
        title="Perfil de Adherencia Nutricional",
        polar=dict(
            radialaxis=dict(
                visible=True,
                range=[0, 100]
            )
        ),
        showlegend=False
    )

    return json.loads(fig.to_json())
