# 동시성 제어에 대한 분석 및 보고

## 동시성 문제란?

동시성 문제는 여러 스레드가 동일한 자원에 동시에 접근하거나 작업할 때 발생할 수 있는 데이터 일관성 및 작업 순서의 예측 불가능성을 의미함.
주로 멀티스레드 환경에서 발생하며, 올바르게 처리하지 않으면 프로그램의 동작이 비정상적이거나 예기치 않은 결과를 초래할 수 있음

## 동시성 제어란?

동시성 제어는 동시성 문제가 발생하지 않도록 여러 스레드가 공유 자원에 안전하게 접근하고 작업을 수행할 수 있도록 스레드간의 작업 순서와
자원 사용을 조율함으로 데이터를 일관성 있고 예측 가능하게 유지하는 것을 의미함

## 자바에서의 동시성 제어 방식

- 분산 환경
    - 분산 환경에서의 동시성 제어는 여러 시스템이나 노드가 동일한 자원에 접근하거나 수행할 때 데이터의 일관성, 무결성을 보장하기 위해서 사용됨
    - 여러 노드가 동시에 동일한 자원에 접근하면 데이터의 중복 처리가 발생할 수 있기 때문에 이를 방지하기 위해서 동시성 제어가 필요함
    - 단일 시스템과 다르게 여러개의 노드들이 동일 작업을 중복 실행하거나 잘못된 순서로 실행하지 않도록 제어 해야함
    - 동시성 제어 방식
        - 분산 락 : 여러 노드들이 동시에 동일한 자원에 접근하지 못하도록 제어하는 메커니즘
            1. DB 를 활용한 분산 락 : 데이터베이스의 트랜잭션과 락 기능을 활용하여 동시에 동일 자원에 접근하지 못하도록 함. `FOR UPDATE` 등을 통한 비관적인 락이나 버전 컬럼을 활용한
               낙관적인 랑 등을 활용 가능함
            2. Redis 를 활용한 분산 락 : 레디스의 빠른 속도와 간단한 Key-Value 저장소를 활용한 동시성 제어 제공. SETNX 나 SET을 활용해서 락을 설정 DELETE 등으로 해제함.
               SPOF 가 될 수 있지만, Redlock 알고리즘 등으로 극복 가능
- 단일 시스템
    - 단일 시스템에서의 동시성 제어는 분산 환경과 다르게 모든 작업이 같은 프로세스에서 이뤄지게 되므로 스레드와 관련한 문제를 해결하는데 초점을 맞추면 됨
    - 요구사항에서 정의한 환경은 단일 시스템 환경으로 아래에서 해당 내용에 대한 분석을 진행

## 요구사항에서 주어진 단일 시스템에서의 동시성 제어 방식

### Mutex(Mutual Exclusion)

임계구역이 있는 스레드들이 실행시간이 겹치지 않고 단독으로 실행되도록 하는것. 즉 한개의 스레드만 임계영역이 들어가서 상호 배제 하는것. 임계 영역을 두개 이상의 스레드가 사용할 수 없음
요구사항의 `동시에 여러 요청이 들어오더라도 순서대로 (혹은 한번에 하나의 요청씩만) 제어될 수 있도록 리팩토링` 을 달성하기 위해서는 상호배제가 필수적

1. Synchronized

- **개념**: 자바 언어 키워드를 이용한 동기화 방식으로, 특정 블록 또는 메서드를 하나의 스레드만 접근 가능하도록 함
- **특징**:
    - 재진입 가능: 같은 스레드가 이미 획득한 모니터(락)를 연속해서 획득할 수 있음
    - 공정성 보장 없음: 기다리는 스레드 순서대로 Lock을 주지 않으며, 특정 스레드가 Lock 획득에 계속 실패하는 기아가 발생할 수 있음
    - 구현이 매우 간단하고 직관적임
- **장단점**:
    - 장점: 사용이 쉽고, 간단한 임계영역 보호에 충분
    - 단점: 공정성 제어 부족, 세밀한 Lock 획득/해제 제어 불가, Condition 기반의 정교한 흐름 제어 어려움

2. ReentrantLock

- **개념**: `java.util.concurrent.locks` 패키지 제공 Lock 구현체로, 명시적으로 `lock()`/`unlock()` 호출이 필요
- **특징**:
    - 재진입 가능, `tryLock()`, `lockInterruptibly()` 등 다양한 Lock 획득 전략 제공
    - `Condition` 객체를 통한 세밀한 대기 및 알림 제어 가능
  - 공정성 옵션을 생성자에서 설정할 수 있어, 스레드가 공평하게 Lock을 획득할 수 있도록 설정할 수 있음
  - 결국 하나의 Lock 객체당 한 번에 한 스레드만 임계영역에 진입할 수 있는 상호배제(Mutex) 메커니즘 제공
- **장단점**:
    - 장점: 세밀한 Lock 정책 설정 가능, 공정성 제어 가능, 조건변수 활용 가능
    - 단점: 명시적 Lock 관리 필요(try-finally), 구현 난이도 증가


### Semaphore

지정된 수의 스레드들이 동시에 특정 자원에 접근할 수 있도록 하는 기법. 여러개의 스레드가 동시에 공유자원(임계구역)에 진입할 수 있음.
뮤텍스와 다르게 한개의 스레드가 책임을 갖는 것이 아니기 때문에 공유 자원에 진입한 스레드가 다른 스레드가 진입 가능하도록 변경할 수 있음

- **개념**: N개의 허용량(permit)을 주어, 최대 N개의 스레드가 동시에 임계영역에 진입할 수 있도록 하는 동시성 제어 기법
- **특징**:
    - `Synchronized`나 `ReentrantLock`이 기본적으로 "한 번에 한 스레드"만 허용하는데 비해, `Semaphore`는 "한 번에 여러 스레드"를 허용
    - 리소스 개수가 제한된 상황(예: 스레드풀, 연결 풀)에서 주로 사용함
    - 뮤텍스는 세마포어가 될 수 없지만, 세마포어는 뮤텍스로써 구현 가능하다. 허용량이 1인 세마포어는 뮤텍스라 할 수 있음
- **장단점**:
    - 장점: 동시 접근 가능한 스레드 수를 유연하게 제어
    - 단점: permit 관리 필요, 단순한 상호배제와 달리 접근 동시성을 조절하는 추가 로직이 필요

## 요구사항 구현

- 위에서 언급한 대로, 주어진 요구사항을 구현하기 위해서는 Mutex 가 필수적이기 때문에 `Synchronized`, `ReentrantLock` 
