
function mostrarRadarNutricional(totales, metas) {

    const categorias = [
        "Calorías",
        "Proteínas",
        "Grasas",
        "Carbohidratos"
    ];

    const claves = [
        "calorias",
        "proteinas",
        "grasas",
        "carbohidratos"
    ];

    const valores = claves.map(clave => {
        const consumo = Number(totales[clave]);
        const meta = Number(metas[clave]);

        if (
            !Number.isFinite(consumo) ||
            !Number.isFinite(meta) ||
            consumo < 0 ||
            meta <= 0
        ) {
            return null;
        }

        return (consumo / meta) * 100;
    });

    if (valores.some(valor => valor === null)) {
        throw new Error(
            "Faltan totales o metas nutricionales válidas."
        );
    }

    // El radar muestra hasta 150%.
    // Los totales originales no se modifican.
    const valoresVisuales = valores.map(
        valor => Math.min(valor, 150)
    );

    return Plotly.react(
        "radarNutricional",
        [{
            type: "scatterpolar",
            r: valoresVisuales,
            theta: categorias,
            fill: "toself",
            name: "Consumo diario",
            hovertext: valores.map(
                valor => valor.toFixed(1) + "% de la meta"
            ),
            hoverinfo: "theta+text"
        },
        {
            type: "scatterpolar",
            r: [100, 100, 100, 100],
            theta: categorias,
            mode: "lines",
            name: "Meta diaria",
            line: {
                dash: "dash"
            }
        }],
        {
            title: "Cumplimiento nutricional diario",
            polar: {
                radialaxis: {
                    visible: true,
                    range: [0, 150],
                    ticksuffix: "%"
                }
            },
            showlegend: true,
            margin: {
                l: 60,
                r: 60,
                t: 60,
                b: 40
            }
        },
        {
            responsive: true,
            displayModeBar: false
        }
    );
}
