package com.entec.tax.common.constants;

/**
 * 조세특례제한법 조항 코드 상수
 */
public final class ProvisionCode {

    private ProvisionCode() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /** 제6조 - 창업중소기업 등에 대한 세액감면 */
    public static final String ART_6 = "§6";

    /** 제7조 - 중소기업에 대한 특별세액감면 */
    public static final String ART_7 = "§7";

    /** 제10조 - 연구·인력개발비에 대한 세액공제 */
    public static final String ART_10 = "§10";

    /** 제24조 - 통합투자세액공제 */
    public static final String ART_24 = "§24";

    /** 제29조의8 - 통합고용세액공제 */
    public static final String ART_29_8 = "§29의8";

    /** 제30조의4 - 사회보험료 세액공제 */
    public static final String ART_30_4 = "§30의4";

    /** 제96조의3 - 착한임대인 세액공제 */
    public static final String ART_96_3 = "§96의3";

    /** 제122조의3 - 성실사업자에 대한 의료비 등 세액공제 */
    public static final String ART_122_3 = "§122의3";

    /** 제126조의6 - 성실신고확인비용에 대한 세액공제 */
    public static final String ART_126_6 = "§126의6";
}
