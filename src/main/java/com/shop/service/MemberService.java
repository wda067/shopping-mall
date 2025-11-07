package com.shop.service;

import com.shop.domain.member.Member;
import com.shop.domain.member.MemberRole;
import com.shop.dto.request.JoinRequest;
import com.shop.dto.request.MemberSearch;
import com.shop.dto.response.CommonResponse;
import com.shop.dto.response.MemberResponse;
import com.shop.exception.EmailAlreadyExists;
import com.shop.exception.MemberNotFound;
import com.shop.repository.member.MemberRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Transactional
    public void join(JoinRequest request) {
        if (memberRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new EmailAlreadyExists();
        }

        String encryptedPassword = passwordEncoder.encode(request.getPassword());
        Member member = Member.builder()
                .email(request.getEmail())
                .name(request.getName())
                .password(encryptedPassword)
                .role(MemberRole.USER)
                .build();

        memberRepository.save(member);
    }

    @Transactional
    public void leave(String email) {
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(MemberNotFound::new);

        memberRepository.delete(member);
    }

    public CommonResponse<MemberResponse> getMember(String email) {
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(MemberNotFound::new);

        return CommonResponse.success(new MemberResponse(member));
    }

    public CommonResponse<List<MemberResponse>> getMembers(MemberSearch request) {
        return CommonResponse.success(memberRepository.findAll().stream()
                .map(MemberResponse::new)
                .toList());
    }
}
