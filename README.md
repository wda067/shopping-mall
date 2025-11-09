# 🛒 쇼핑몰 프로젝트

## 📌프로젝트 소개

본 프로젝트는 상품을 주문하고 결제할 수 있는 쇼핑몰 서비스입니다.

## 📌주요 기능

- 회원은 상품을 주문하고 토스페이먼츠 API를 통해 결제를 진행할 수 있습니다.
- 주문 완료 시 이메일을 비동기적으로 전송합니다.
- 트랜잭션 범위 최적화, 분산락, 후보정, 이벤트 기반 아키텍처를 통해 안정적이고 일관성 있는 주문·결제 시스템을 구축했습니다.

## 💻기술 스택

<p>
  <!-- Java -->
  <img src="https://img.shields.io/badge/Java-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white" />
  <!-- Spring -->
  <img src="https://img.shields.io/badge/Spring Boot-6DB33F?style=for-the-badge&logo=spring-boot&logoColor=white" />
  <!-- MySQL -->
  <img src="https://img.shields.io/badge/MySQL-005C84?style=for-the-badge&logo=mysql&logoColor=white" />
  <!-- Redis -->
  <img src="https://img.shields.io/badge/redis-%23DD0031.svg?&style=for-the-badge&logo=redis&logoColor=white" />
  <!-- Docker -->
  <img src="https://img.shields.io/badge/docker-%230db7ed.svg?style=for-the-badge&logo=docker&logoColor=white" />
  <!-- Nginx -->
  <img src="https://img.shields.io/badge/nginx-%23009639.svg?style=for-the-badge&logo=nginx&logoColor=white" />
</p>

## 🏗️시스템 아키텍처

<img src="https://github.com/user-attachments/assets/1393d53d-2df4-492d-b290-a04d2eb7334f" width="60%" height="60%" />

## 💡기술적 고민
- 🔗 [**대규모 데이터 조회 성능 최적화**](https://velog.io/@wda067/Spring-Boot-%EB%8C%80%EA%B7%9C%EB%AA%A8-%EC%A3%BC%EB%AC%B8-%EB%8D%B0%EC%9D%B4%ED%84%B0-%EC%84%B1%EB%8A%A5-%EC%B5%9C%EC%A0%81%ED%99%94)
    - 복합 인덱스, 통계 테이블, 비정규화를 적용해 조회 성능을 개선하고 N+1 문제를 제거했습니다.
- 🔗 [**Redis 기반 분산락으로 동시성 제어**](https://velog.io/@wda067/%ED%8A%B8%EB%9E%9C%EC%9E%AD%EC%85%98-%EB%B2%94%EC%9C%84-%EC%B5%9C%EC%A0%81%ED%99%94%ED%9B%84%EB%B3%B4%EC%A0%95%EC%9D%B4%EB%B2%A4%ED%8A%B8-%EA%B8%B0%EB%B0%98%EC%9C%BC%EB%A1%9C-%EC%A3%BC%EB%AC%B8%EA%B2%B0%EC%A0%9C-%EC%8B%9C%EC%8A%A4%ED%85%9C-%EA%B0%9C%EC%84%A0%ED%95%98%EA%B8%B0#%EB%AC%B8%EC%A0%9C%EC%A0%901---%EC%A3%BC%EB%AC%B8-%EC%9A%94%EC%B2%AD-api) 
  - `Redisson`의 `tryLock()`을 활용해 멀티 인스턴스 환경에서도 재고 정합성을 보장했습니다.
- 🔗 [**외부 결제 API 장애 대응 및 데이터 정합성 보장**](https://velog.io/@wda067/%ED%8A%B8%EB%9E%9C%EC%9E%AD%EC%85%98-%EB%B2%94%EC%9C%84-%EC%B5%9C%EC%A0%81%ED%99%94%ED%9B%84%EB%B3%B4%EC%A0%95%EC%9D%B4%EB%B2%A4%ED%8A%B8-%EA%B8%B0%EB%B0%98%EC%9C%BC%EB%A1%9C-%EC%A3%BC%EB%AC%B8%EA%B2%B0%EC%A0%9C-%EC%8B%9C%EC%8A%A4%ED%85%9C-%EA%B0%9C%EC%84%A0%ED%95%98%EA%B8%B0#%EB%AC%B8%EC%A0%9C%EC%A0%902---%EC%99%B8%EB%B6%80-api)
    - 결제 요청과 DB 반영을 분리하고, 실패 시 자동 보정 스케줄러로 누락된 결제 내역을 복원했습니다.
- 🔗 [**이벤트 기반 비동기 아키텍처 적용**](https://velog.io/@wda067/%ED%8A%B8%EB%9E%9C%EC%9E%AD%EC%85%98-%EB%B2%94%EC%9C%84-%EC%B5%9C%EC%A0%81%ED%99%94%ED%9B%84%EB%B3%B4%EC%A0%95%EC%9D%B4%EB%B2%A4%ED%8A%B8-%EA%B8%B0%EB%B0%98%EC%9C%BC%EB%A1%9C-%EC%A3%BC%EB%AC%B8%EA%B2%B0%EC%A0%9C-%EC%8B%9C%EC%8A%A4%ED%85%9C-%EA%B0%9C%EC%84%A0%ED%95%98%EA%B8%B0#%EB%AC%B8%EC%A0%9C%EC%A0%903---%EC%95%8C%EB%A6%BC-%EC%A0%84%EC%86%A1)
    - `@TransactionalEventListener`와 `@Async`로 주문 완료 이메일을 비동기 처리해 트랜잭션 안정성과 응답 속도를 개선했습니다.
