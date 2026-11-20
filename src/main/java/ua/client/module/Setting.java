package ua.client.module;

public class Setting {
    public final String name;
    public final boolean isBool;
    public boolean on;
    public double value, min, max, step;
    public String[] modes;

    private Setting(String name, boolean isBool) { this.name = name; this.isBool = isBool; }

    public static Setting num(String name, double def, double min, double max, double step) {
        Setting s = new Setting(name, false);
        s.value = def; s.min = min; s.max = max; s.step = step;
        return s;
    }

    public static Setting bool(String name, boolean def) {
        Setting s = new Setting(name, true);
        s.on = def;
        return s;
    }

    public static Setting mode(String name, int def, String... modes) {
        Setting s = num(name, def, 0, modes.length - 1, 1);
        s.modes = modes;
        return s;
    }

    public void change(boolean up) {
        if (isBool) { on = !on; return; }
        value += up ? step : -step;
        if (value > max + 1e-9) value = min;
        if (value < min - 1e-9) value = max;
        value = Math.round(value / step) * step;
    }

    public int i() { return (int) Math.round(value); }
    public float f() { return (float) value; }

    public String display() {
        if (isBool) return on ? "ON" : "OFF";
        if (modes != null) return modes[Math.max(0, Math.min(modes.length - 1, i()))];
        if (step >= 1) return String.valueOf(i());
        return String.format(java.util.Locale.ROOT, "%.2f", value);
    }
}
