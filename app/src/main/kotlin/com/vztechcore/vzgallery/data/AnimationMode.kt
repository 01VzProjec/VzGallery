package com.vztechcore.vzgallery.data

enum class AnimationMode(val label: String, val description: String) {
    ANIM_1(
        label = "Animação 1 (padrão)",
        description = "Fade + escala suave ao abrir menus e trocar de tela"
    ),
    ANIM_2(
        label = "Animação 2 (transições)",
        description = "Telas e menus deslizam lateralmente, como se passassem por cima"
    ),
    NONE(
        label = "Sem animação",
        description = "Tudo aparece instantaneamente, sem transição"
    )
}