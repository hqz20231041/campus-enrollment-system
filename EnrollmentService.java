package com.campus.enrollment.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.campus.enrollment.vo.request.EnrollmentRequest;
import com.campus.enrollment.vo.response.EnrollmentResult;
import com.campus.enrollment.mapper.EnrollmentMapper;
import com.campus.enrollment.entity.Enrollment;

@Service
@Slf4j
public class EnrollmentService {

    @Autowired
    private EnrollmentMapper enrollmentMapper;

    @Autowired
    private AsyncEnrollmentService asyncEnrollmentService;

    @Autowired
    private ActivityService activityService;

    @Autowired
    private AntiSpamService antiSpamService;

    /**
     * 提交报名申请
     */
    public EnrollmentResult submitEnrollment(EnrollmentRequest request) {
        try {
            // 1. 防刷检测
            if (antiSpamService.checkSpamRisk(request)) {
                return EnrollmentResult.fail("操作过于频繁，请稍后重试");
            }

            // 2. 活动校验
            if (!activityService.isActivityAvailable(request.getActivityId())) {
                return EnrollmentResult.fail("活动不存在或已结束");
            }

            // 3. 名额检查
            if (!activityService.hasAvailableQuota(request.getActivityId())) {
                return EnrollmentResult.fail("活动名额已满");
            }

            // 4. 异步处理报名
            return asyncEnrollmentService.submitEnrollment(request);

        } catch (Exception e) {
            log.error("报名处理异常", e);
            return EnrollmentResult.fail("系统繁忙，请稍后重试");
        }
    }

    /**
     * 同步处理报名（降级方案）
     */
    @Transactional
    public EnrollmentResult processEnrollmentSync(EnrollmentRequest request) {
        try {
            // 分布式锁防止重复提交
            String lockKey = String.format("enroll_lock:%s:%s", 
                request.getUserId(), request.getActivityId());
            
            // 检查是否已报名
            if (enrollmentMapper.existsByUserAndActivity(
                request.getUserId(), request.getActivityId())) {
                return EnrollmentResult.fail("您已报名该活动");
            }

            // 创建报名记录
            Enrollment enrollment = new Enrollment();
            enrollment.setEnrollmentId(generateEnrollmentId());
            enrollment.setUserId(request.getUserId());
            enrollment.setActivityId(request.getActivityId());
            enrollment.setCollegeId(request.getCollegeId());
            enrollment.setStatus(1); // 待审核
            enrollment.setCreateTime(new Date());

            enrollmentMapper.insert(enrollment);

            // 更新活动名额
            activityService.incrementEnrollmentCount(request.getActivityId());

            return EnrollmentResult.success("报名成功", enrollment.getEnrollmentId());

        } catch (Exception e) {
            log.error("同步报名处理失败", e);
            throw new RuntimeException("报名处理失败");
        }
    }

    private String generateEnrollmentId() {
        return "EN" + System.currentTimeMillis() + 
               String.format("%04d", (int)(Math.random() * 10000));
    }
}
