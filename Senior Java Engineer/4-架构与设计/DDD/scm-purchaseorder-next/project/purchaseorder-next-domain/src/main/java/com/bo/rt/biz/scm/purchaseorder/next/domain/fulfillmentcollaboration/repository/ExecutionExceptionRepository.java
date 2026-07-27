package com.bo.rt.biz.scm.purchaseorder.next.domain.fulfillmentcollaboration.repository;

import com.bo.rt.biz.scm.purchaseorder.next.domain.fulfillmentcollaboration.model.ExecutionExceptionCase;
import java.util.Optional;

/** 履约异常聚合的持久化边界。 */
public interface ExecutionExceptionRepository {

    /** 保存异常处理单。 */
    void save(ExecutionExceptionCase exceptionCase);

    /** 按稳定业务键查找尚未关闭的异常，防止重复开单。 */
    Optional<ExecutionExceptionCase> findOpenByBusinessKey(String businessKey);
}
