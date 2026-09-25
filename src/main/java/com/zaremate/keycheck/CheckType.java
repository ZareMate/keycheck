package com.zaremate.keycheck;

public enum CheckType {
    KEYBIND,
    TRANSLATE,
    METEOR;

    public static CheckType parse(String value) {
        try {
            return valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return KEYBIND;
        }
    }
}
