package tn.techstore.common;

/** Erreur métier renvoyée au client sous forme {"message": "..."} avec le code HTTP donné. */
public class ApiException extends RuntimeException {

    private final int status;

    public ApiException(int status, String message) {
        super(message);
        this.status = status;
    }

    public int status() {
        return status;
    }

    public static ApiException notFound(String what) {
        return new ApiException(404, what + " introuvable");
    }

    public static ApiException badRequest(String message) {
        return new ApiException(400, message);
    }

    public static ApiException conflict(String message) {
        return new ApiException(409, message);
    }
}
