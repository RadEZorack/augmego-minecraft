package com.augmego.avatar.core;

public class AvatarLoadException extends Exception {
    public AvatarLoadException(String message) {
        super(message);
    }

    public AvatarLoadException(String message, Throwable cause) {
        super(message, cause);
    }
}
