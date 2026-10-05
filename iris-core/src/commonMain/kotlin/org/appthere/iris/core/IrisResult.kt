package org.appthere.iris.core

/** The outcome of an operation that can fail in an expected way; programmer errors use `require`/`check` instead. */
public sealed interface IrisResult<out T> {
    public data class Success<out T>(
        val value: T,
    ) : IrisResult<T>

    public data class Failure(
        val error: IrisError,
    ) : IrisResult<Nothing>
}

public inline fun <T, R> IrisResult<T>.map(transform: (T) -> R): IrisResult<R> =
    when (this) {
        is IrisResult.Success -> IrisResult.Success(transform(value))
        is IrisResult.Failure -> this
    }

public inline fun <T, R> IrisResult<T>.flatMap(transform: (T) -> IrisResult<R>): IrisResult<R> =
    when (this) {
        is IrisResult.Success -> transform(value)
        is IrisResult.Failure -> this
    }

public inline fun <T, R> IrisResult<T>.fold(
    onSuccess: (T) -> R,
    onFailure: (IrisError) -> R,
): R =
    when (this) {
        is IrisResult.Success -> onSuccess(value)
        is IrisResult.Failure -> onFailure(error)
    }

public fun <T> IrisResult<T>.getOrNull(): T? = (this as? IrisResult.Success)?.value

public fun IrisResult<*>.errorOrNull(): IrisError? = (this as? IrisResult.Failure)?.error

public inline fun <T> IrisResult<T>.getOrElse(onFailure: (IrisError) -> T): T = fold({ it }, onFailure)

public inline fun <T> IrisResult<T>.onSuccess(action: (T) -> Unit): IrisResult<T> {
    if (this is IrisResult.Success) action(value)
    return this
}

public inline fun <T> IrisResult<T>.onFailure(action: (IrisError) -> Unit): IrisResult<T> {
    if (this is IrisResult.Failure) action(error)
    return this
}
