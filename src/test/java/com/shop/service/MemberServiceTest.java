package com.shop.service;

import static org.junit.jupiter.api.Assertions.*;

import com.shop.config.TestSecurityConfig;
import com.shop.domain.member.Member;
import com.shop.dto.request.JoinRequest;
import com.shop.exception.EmailAlreadyExists;
import com.shop.repository.member.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootTest
@Import(TestSecurityConfig.class)
class MemberServiceTest {

    @Autowired
    private MemberService memberService;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        memberRepository.deleteAll();
    }

    @Test
    void 회원가입에_성공한다() {
        //given
        JoinRequest request = new JoinRequest(
                "test@test.com",
                "test",
                "password");

        //when
        memberService.join(request);

        //then
        Member findMember = memberRepository.findAll().get(0);
        assertEquals(1, memberRepository.count());
        assertEquals("test@test.com", findMember.getEmail());
        assertEquals("test", findMember.getName());
        assertTrue(passwordEncoder.matches("password", findMember.getPassword()));
    }

    @Test
    void 중복된_이메일으로_회원가입할_수_없다() {
        //given
        Member member = Member.builder()
                .email("test@test.com")
                .build();
        memberRepository.save(member);

        JoinRequest request = new JoinRequest(
                "test@test.com",
                "test",
                "password");

        //expected
        assertThrows(EmailAlreadyExists.class, () -> memberService.join(request));
    }

    @Test
    void 회원탈퇴에_성공한다() {
        //given
        Member member = Member.builder()
                .email("test@test.com")
                .build();
        memberRepository.save(member);

        //when
        memberService.leave(member.getEmail());

        //then
        assertEquals(0, memberRepository.count());
        assertTrue(memberRepository.findByEmail("test@test.com").isEmpty());
    }
}