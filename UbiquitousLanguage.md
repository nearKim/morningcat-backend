# Ubiquitous Language - MorningCat (아침형 고양이) 도메인 용어집

> 이 문서는 MorningCat 프로젝트의 살아있는 용어집입니다. 모든 팀원과 AI 에이전트는 이 용어를 엄격하게 준수해야 합니다.
> 새로운 도메인 개념이 도입될 때마다 이 문서를 업데이트해야 합니다.

## 용어 정의

| 용어           | 영문명                   | 정의                                                        | 바운디드 컨텍스트                  |
|--------------|-----------------------|-----------------------------------------------------------|----------------------------|
| **사용자**      | User                  | 매일 아침 정보를 받기 위해 서비스에 등록한 개인.                              | 사용자 관리 (User Management)   |
| **사용자 ID**   | UserId                | 사용자의 고유 식별자. UUID 형식.                                     | 사용자 관리                     |
| **구독**       | Subscription          | 사용자와 MorningCat 서비스 간의 활성 관계. 일일 배송 접근 권한을 결정함.           | 사용자 관리                     |
| **구독 상태**    | SubscriptionStatus    | 구독의 현재 상태. Trial/Active/Suspended/Cancelled 중 하나.         | 사용자 관리                     |
| **사용자 환경설정** | UserPreferences       | 사용자가 받는 콘텐츠와 시간을 결정하는 사용자 정의 설정.                          | 사용자 관리                     |
| **배송 시간**    | DeliveryTime          | 사용자가 일일 다이제스트를 받기 원하는 로컬 시간. HH:MM 형식.                    | 사용자 관리, 일정 관리 (Scheduling) |
| **시간대**      | TimeZone              | 사용자의 로컬 시간을 결정하는 지리적 시간대. IANA 식별자 사용.                    | 사용자 관리                     |
| **위치**       | Location              | 날씨 및 지역 추천을 위한 사용자의 도시/지역 정보.                             | 사용자 관리                     |
| **일일 다이제스트** | DailyDigest           | 특정 날짜에 특정 사용자를 위해 준비된 완전한 정보 패키지.                         | 콘텐츠 (Content)              |
| **콘텐츠 섹션**   | ContentSection        | 일일 다이제스트 내의 관련 정보의 논리적 그룹. (예: 뉴스 섹션, 날씨 섹션)              | 콘텐츠                        |
| **콘텐츠 카테고리** | ContentCategory       | 사용자가 활성화하거나 비활성화할 수 있는 콘텐츠 유형의 상위 분류.                     | 콘텐츠                        |
| **콘텐츠 항목**   | ContentItem           | 콘텐츠 섹션 내의 단일 정보 조각.                                       | 콘텐츠                        |
| **콘텐츠 점수**   | ContentScore          | 특정 사용자에 대한 콘텐츠 항목의 관련성을 나타내는 0.0-1.0 범위의 숫자.              | 콘텐츠, 개인화 (Personalization) |
| **평일 콘텐츠**   | WeekdayContent        | 주중(월-금)에 제공되는 콘텐츠 유형. 뉴스, 경제, 일정, 생산성 팁 포함.               | 콘텐츠                        |
| **주말 콘텐츠**   | WeekendContent        | 주말과 공휴일에 제공되는 콘텐츠 유형. 여가, 웰빙, 문화 추천 포함.                   | 콘텐츠                        |
| **배송 채널**    | DeliveryChannel       | 일일 다이제스트가 사용자에게 도달하는 매체. EMAIL/PUSH_NOTIFICATION/IN_APP.  | 알림 (Notification)          |
| **배송 시도**    | DeliveryAttempt       | 특정 채널을 통해 사용자에게 일일 다이제스트를 전달하려는 단일 시도.                    | 알림                         |
| **배송 상태**    | DeliveryStatus        | 배송 시도의 현재 상태. Pending/InProgress/Successful/Failed/Retry. | 알림                         |
| **배송 창**     | DeliveryWindow        | 사용자의 선호 시간 기준 ±15분의 허용 가능한 배송 시간 범위.                      | 알림, 일정 관리                  |
| **알림 템플릿**   | NotificationTemplate  | 특정 배송 채널용 일일 다이제스트 콘텐츠 렌더링을 위한 사전 정의된 형식.                 | 알림                         |
| **배송 일정**    | DeliverySchedule      | 사용자가 일일 다이제스트를 받는 시기를 결정하는 타이밍 구성.                        | 일정 관리                      |
| **평일 규칙**    | WorkdayRule           | 주어진 날짜가 콘텐츠 선택 목적상 평일인지 판단하는 로직.                          | 일정 관리                      |
| **공휴일**      | Holiday               | 콘텐츠 배송에 영향을 미치는 특정 국가의 공휴일.                               | 일정 관리                      |
| **배송 작업**    | DeliveryJob           | 사용자 배치의 일일 다이제스트를 처리하고 전송하는 시스템의 예약된 작업.                  | 일정 관리                      |
| **조용한 시간**   | QuietHours            | 사용자 선호와 관계없이 배송이 금지된 시간대. 기본값: 22:00-05:00.               | 일정 관리                      |
| **콘텐츠 제공자**  | ContentProvider       | 일일 다이제스트에 포함될 원시 콘텐츠를 공급하는 외부 서비스.                        | 통합 (Integration)           |
| **콘텐츠 캐시**   | ContentCache          | API 호출을 줄이고 성능을 향상시키기 위한 외부 콘텐츠의 임시 저장소.                  | 통합                         |
| **통합 자격증명**  | IntegrationCredential | 외부 서비스 접근을 위한 사용자별 인증 데이터.                                | 통합                         |
| **속도 제한 정책** | RateLimitPolicy       | 외부 콘텐츠 제공자에 대한 요청 빈도를 관리하는 규칙.                            | 통합                         |
| **데이터 변환기**  | DataTransformer       | 원시 외부 데이터를 표준화된 콘텐츠 항목으로 변환하는 구성 요소.                      | 통합                         |
| **사용자 프로필**  | UserProfile           | 콘텐츠 개인화에 사용되는 축적된 행동 및 선호도 데이터.                           | 개인화                        |
| **개인화 규칙**   | PersonalizationRule   | 사용자 행동, 선호도 또는 컨텍스트를 기반으로 콘텐츠 선택을 맞춤화하는 로직.               | 개인화                        |
| **개인화 엔진**   | PersonalizationEngine | 각 사용자를 위해 콘텐츠를 선택하고 순위를 매기는 AI 기반 시스템.                    | 개인화                        |
| **참여 지표**    | EngagementMetric      | 전달된 콘텐츠와의 측정 가능한 사용자 상호작용.                                | 개인화                        |
| **최소 콘텐츠**   | MinimumContent        | 유효한 일일 다이제스트에 필요한 최소 콘텐츠 양. 최소 3개 섹션, 각 2개 항목.            | 콘텐츠                        |
| **콘텐츠 신선도**  | ContentFreshness      | 일일 다이제스트에 포함될 수 있는 콘텐츠의 최대 연령.                            | 콘텐츠                        |

## 바운디드 컨텍스트별 용어 분류

### 사용자 관리 (User Management)

- 사용자 (User)
- 사용자 ID (UserId)
- 구독 (Subscription)
- 구독 상태 (SubscriptionStatus)
- 사용자 환경설정 (UserPreferences)
- 배송 시간 (DeliveryTime)
- 시간대 (TimeZone)
- 위치 (Location)

### 콘텐츠 (Content)

- 일일 다이제스트 (DailyDigest)
- 콘텐츠 섹션 (ContentSection)
- 콘텐츠 카테고리 (ContentCategory)
- 콘텐츠 항목 (ContentItem)
- 콘텐츠 점수 (ContentScore)
- 평일 콘텐츠 (WeekdayContent)
- 주말 콘텐츠 (WeekendContent)
- 최소 콘텐츠 (MinimumContent)
- 콘텐츠 신선도 (ContentFreshness)

### 알림 (Notification)

- 배송 채널 (DeliveryChannel)
- 배송 시도 (DeliveryAttempt)
- 배송 상태 (DeliveryStatus)
- 배송 창 (DeliveryWindow)
- 알림 템플릿 (NotificationTemplate)

### 일정 관리 (Scheduling)

- 배송 시간 (DeliveryTime)
- 배송 창 (DeliveryWindow)
- 배송 일정 (DeliverySchedule)
- 평일 규칙 (WorkdayRule)
- 공휴일 (Holiday)
- 배송 작업 (DeliveryJob)
- 조용한 시간 (QuietHours)

### 통합 (Integration)

- 콘텐츠 제공자 (ContentProvider)
- 콘텐츠 캐시 (ContentCache)
- 통합 자격증명 (IntegrationCredential)
- 속도 제한 정책 (RateLimitPolicy)
- 데이터 변환기 (DataTransformer)

### 개인화 (Personalization)

- 사용자 프로필 (UserProfile)
- 콘텐츠 점수 (ContentScore)
- 개인화 규칙 (PersonalizationRule)
- 개인화 엔진 (PersonalizationEngine)
- 참여 지표 (EngagementMetric)

## 도메인 이벤트

| 이벤트명          | 영문명                | 발생 시점                   | 관련 컨텍스트 |
|---------------|--------------------|-------------------------|---------|
| **사용자 등록됨**   | UserRegistered     | 새 사용자가 등록을 완료했을 때       | 사용자 관리  |
| **환경설정 변경됨**  | PreferencesUpdated | 사용자가 환경설정을 수정했을 때       | 사용자 관리  |
| **다이제스트 생성됨** | DigestGenerated    | 일일 다이제스트가 성공적으로 생성되었을 때 | 콘텐츠     |
| **배송 완료됨**    | DeliveryCompleted  | 일일 다이제스트가 성공적으로 전달되었을 때 | 알림      |
| **배송 실패**     | DeliveryFailed     | 모든 재시도 후 배송이 실패했을 때     | 알림      |
| **콘텐츠 참여됨**   | ContentEngaged     | 사용자가 전달된 콘텐츠와 상호작용했을 때  | 개인화     |

## 사용 지침

1. **일관성**: 코드, 문서, 대화에서 이 용어집의 용어를 일관되게 사용하세요.
2. **정확성**: 용어의 의미를 임의로 변경하지 마세요. 변경이 필요한 경우 팀 논의를 거쳐 이 문서를 업데이트하세요.
3. **컨텍스트 인식**: 같은 용어라도 바운디드 컨텍스트에 따라 다른 속성을 가질 수 있음을 인지하세요.
4. **지속적 업데이트**: 새로운 개념이 도입되면 즉시 이 문서에 추가하세요.

## 변경 이력

| 날짜         | 변경 내용                   | 작성자    |
|------------|-------------------------|--------|
| 2025-07-04 | 초기 버전 생성 - 핵심 도메인 용어 정의 | System |
| 2025-07-04 | 도메인 이벤트 섹션 추가           | System |

---

> **Note**: 이 문서는 DDD의 핵심 실천법인 유비쿼터스 언어를 구현하는 중요한 아키텍처 산출물입니다.
> 모든 도메인 모델링과 코드 구현은 이 용어집을 기반으로 해야 합니다.