package com.example.clientapp.presentation.state

import com.example.clientapp.domain.model.NetworkError
import com.example.clientapp.domain.model.ValidationError
import com.example.clientapp.domain.usecase.SendMessageUseCase

/** Domain failure -> presentation error. The ViewModel's only error translation. */
fun Throwable.toUiError(): UiError = when (this) {
    is NetworkError.ConnectionRefused -> UiError.ConnectionRefused
    is NetworkError.Timeout -> UiError.Timeout
    is NetworkError.ServerUnavailable -> UiError.ServerUnavailable
    is NetworkError.NoNetwork -> UiError.NoNetwork
    is NetworkError.MalformedResponse -> UiError.MalformedResponse
    is NetworkError.Http -> UiError.HttpError(code)
    is NetworkError.Unknown -> UiError.Unknown(detail)
    is ValidationError -> UiError.Validation(message ?: "Invalid input")
    SendMessageUseCase.EmptyMessageError -> UiError.Validation("Enter a message first")
    else -> UiError.Unknown(message ?: "Something went wrong")
}
