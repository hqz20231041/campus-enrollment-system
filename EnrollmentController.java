package com.campus.enrollment.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.campus.enrollment.service.EnrollmentService;
import com.campus.enrollment.vo.request.EnrollmentRequest;
import com.campus.enrollment.vo.response.EnrollmentResult;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/enrollment")
@Slf4j
public class EnrollmentController {

    @Autowired
    private EnrollmentService enrollmentService;

    /**
     * 提交报名申请
     */
    @PostMapping
    public EnrollmentResult enroll(@Valid @RequestBody EnrollmentRequest request) {
        log.info("收到报名请求: 用户ID={}, 活动ID={}", 
            request.getUserId(), request.getActivityId());
        return enrollmentService.submitEnrollment(request);
    }

    /**
     * 查询报名状态
     */
    @GetMapping("/{enrollmentId}")
    public EnrollmentResult getEnrollmentStatus(@PathVariable String enrollmentId) {
        return enrollmentService.getEnrollmentStatus(enrollmentId);
    }

    /**
     * 取消报名
     */
    @DeleteMapping("/{enrollmentId}")
    public EnrollmentResult cancelEnrollment(@PathVariable String enrollmentId) {
        return enrollmentService.cancelEnrollment(enrollmentId);
    }
}
