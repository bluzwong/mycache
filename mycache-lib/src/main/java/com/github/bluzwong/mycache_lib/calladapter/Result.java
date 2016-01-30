package com.github.bluzwong.mycache_lib.calladapter;

/**
 * Created by Bruce-Home on 2016/1/31.
 */

import retrofit2.Response;

import java.io.IOException;

import static com.github.bluzwong.mycache_lib.calladapter.Utils.checkNotNull;

/** The result of executing an HTTP request. */
public final class Result<T> {
    public static <T> Result<T> error(Throwable error) {
        return new Result<>(null, checkNotNull(error, "error == null"));
    }

    public static <T> Result<T> response(Response<T> response) {
        return new Result<>(checkNotNull(response, "response == null"), null);
    }

    private final Response<T> response;
    private final Throwable error;

    Result(Response<T> response, Throwable error) {
        this.response = response;
        this.error = error;
    }

    /**
     * The response received from executing an HTTP request. Only present when {@link #isError()} is
     * false, null otherwise.
     */
    public Response<T> response() {
        return response;
    }

    /**
     * The error experienced while attempting to execute an HTTP request. Only present when {@link
     * #isError()} is true, null otherwise.
     * <p>
     * If the error is an {@link IOException} then there was a problem with the transport to the
     * remote server. Any other exception type indicates an unexpected failure and should be
     * considered fatal (configuration error, programming error, etc.).
     */
    public Throwable error() {
        return error;
    }

    /** {@code true} if the request resulted in an error. See {@link #error()} for the cause. */
    public boolean isError() {
        return error != null;
    }
}
