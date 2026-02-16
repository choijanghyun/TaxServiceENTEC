package com.entec.tax.domain.request.repository;

import com.entec.tax.domain.request.entity.ReqRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import javax.persistence.LockModeType;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * REQ_REQUEST 테이블 리포지토리.
 * <p>
 * 세액공제 환급 요청 건의 CRUD 및 조회를 담당한다.
 * </p>
 */
public interface ReqRequestRepository extends JpaRepository<ReqRequest, String> {

    /**
     * seq_no 발급을 위한 SELECT FOR UPDATE.
     * 동일 신청인·동일 요청일의 최대 연번을 비관적 잠금으로 조회한다.
     *
     * @param applicantId  신청자 식별번호
     * @param requestDate  요청 접수일
     * @return 최대 seq_no (없으면 Optional.empty)
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT MAX(r.seqNo) FROM ReqRequest r WHERE r.applicantId = :applicantId AND r.requestDate = :requestDate")
    Optional<Integer> findMaxSeqNoForUpdate(@Param("applicantId") String applicantId,
                                            @Param("requestDate") LocalDate requestDate);

    /**
     * 상태별 요청 목록 조회 (최신순).
     *
     * @param status 요청 처리 상태
     * @return 해당 상태의 요청 목록
     */
    List<ReqRequest> findByRequestStatusOrderByCreatedAtDesc(String status);

    /**
     * 신청인별 요청 이력 조회 (최신순).
     *
     * @param applicantId 신청자 식별번호
     * @return 해당 신청인의 요청 목록
     */
    List<ReqRequest> findByApplicantIdOrderByCreatedAtDesc(String applicantId);

    /**
     * 요청 상태를 갱신한다.
     *
     * @param reqId         요청 ID
     * @param requestStatus 변경할 상태
     * @param modifiedAt    수정 일시
     * @return 갱신된 레코드 수
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE ReqRequest r SET r.requestStatus = :requestStatus, r.modifiedAt = :modifiedAt WHERE r.reqId = :reqId")
    int updateStatus(@Param("reqId") String reqId,
                     @Param("requestStatus") String requestStatus,
                     @Param("modifiedAt") LocalDateTime modifiedAt);

    /**
     * 요청 상태와 오류 메시지를 동시에 갱신한다.
     *
     * @param reqId        요청 ID
     * @param status       변경할 상태
     * @param errorMessage 오류 메시지
     * @param modifiedAt   수정 일시
     * @return 갱신된 레코드 수
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE ReqRequest r SET r.requestStatus = :status, r.errorMessage = :errorMessage, r.modifiedAt = :modifiedAt WHERE r.reqId = :reqId")
    int updateStatusWithError(@Param("reqId") String reqId,
                              @Param("status") String status,
                              @Param("errorMessage") String errorMessage,
                              @Param("modifiedAt") LocalDateTime modifiedAt);
}
