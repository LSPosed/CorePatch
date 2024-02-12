package toolkit.coderstory;

public class TrustedCert {
    String hex;
    String desc;
    boolean enabled;

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof TrustedCert) {
            return ((TrustedCert) obj).hex.equals(this.hex);
        }
        return super.equals(obj);
    }
}
