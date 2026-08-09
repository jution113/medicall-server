package juton113.Medicall.global.auth.local;

import juton113.Medicall.global.auth.enums.Role;

import java.util.Map;

public final class LocalTestMember {
    public static final String PROVIDER = "local";

    public static final Map<Role, LocalTestMemberSpec> BY_ROLE = Map.of(
            Role.BASIC, new LocalTestMemberSpec("local-test-basic@medicall.local", "로컬 테스트 유저(BASIC)", "local-test-basic"),
            Role.VERIFIED, new LocalTestMemberSpec("local-test-verified@medicall.local", "로컬 테스트 유저(VERIFIED)", "local-test-verified"),
            Role.ADMIN, new LocalTestMemberSpec("local-test-admin@medicall.local", "로컬 테스트 유저(ADMIN)", "local-test-admin")
    );

    public record LocalTestMemberSpec(String email, String name, String providerId) {
    }

    private LocalTestMember() {
    }
}
