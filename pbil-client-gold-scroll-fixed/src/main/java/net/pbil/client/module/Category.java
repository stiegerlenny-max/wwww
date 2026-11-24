package net.pbil.client.module;

public enum Category {
    RENDER("Render"),
    VISUALS("Visuals"),
    CLIENT("Client"),
    KOMFORT("Komfort");

    public final String label;

    Category(String label) {
        this.label = label;
    }
}
