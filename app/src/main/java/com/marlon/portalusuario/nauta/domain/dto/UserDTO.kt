package com.marlon.portalusuario.nauta.domain.dto

data class UserDTO(
    val id: Int = 0,
    val username: String,
    val password: String,
    val captchaCode: String?
)
