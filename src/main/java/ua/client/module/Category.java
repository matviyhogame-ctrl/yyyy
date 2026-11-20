package ua.client.module;

public enum Category {
    COMBAT("Combat"), MOVEMENT("Movement"), PLAYER("Player"), RENDER("Render"), MISC("Misc");
    public final String label;
    Category(String label) { this.label = label; }
}
