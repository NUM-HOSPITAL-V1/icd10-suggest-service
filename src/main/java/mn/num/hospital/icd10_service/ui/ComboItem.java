package mn.num.hospital.icd10_service.ui;

class ComboItem {
    private String value; // бодит утга (I, II, ...)
    private String label; // UI дээр харагдах

    public ComboItem(String value, String label) {
        this.value = value;
        this.label = label;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return label; // JComboBox дээр харагдах
    }
}