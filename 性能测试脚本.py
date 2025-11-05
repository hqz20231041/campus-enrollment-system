import requests
import threading
import time
import random

class EnrollmentPerformanceTest:
    def __init__(self, base_url, user_count=1000):
        self.base_url = base_url
        self.user_count = user_count
        self.success_count = 0
        self.failure_count = 0
        self.lock = threading.Lock()
    
    def test_enrollment(self, user_id, activity_id):
        url = f"{self.base_url}/api/enrollment"
        payload = {
            "userId": user_id,
            "activityId": activity_id,
            "collegeId": random.randint(1, 16),
            "clientIp": f"192.168.1.{random.randint(1, 255)}"
        }
        
        try:
            start_time = time.time()
            response = requests.post(url, json=payload, timeout=10)
            end_time = time.time()
            
            with self.lock:
                if response.status_code == 200:
                    self.success_count += 1
                    print(f"用户 {user_id} 报名成功, 耗时: {end_time - start_time:.2f}s")
                else:
                    self.failure_count += 1
                    print(f"用户 {user_id} 报名失败: {response.text}")
                    
        except Exception as e:
            with self.lock:
                self.failure_count += 1
            print(f"用户 {user_id} 请求异常: {str(e)}")
    
    def run_test(self, activity_id, thread_count=100):
        threads = []
        start_time = time.time()
        
        # 创建并启动线程
        for i in range(self.user_count):
            user_id = 1000 + i
            thread = threading.Thread(
                target=self.test_enrollment, 
                args=(user_id, activity_id)
            )
            threads.append(thread)
            
            # 控制并发线程数
            if len(threads) >= thread_count:
                for t in threads:
                    t.start()
                for t in threads:
                    t.join()
                threads = []
        
        # 等待剩余线程完成
        for t in threads:
            t.start()
        for t in threads:
            t.join()
            
        end_time = time.time()
        
        # 输出测试结果
        print(f"\n=== 性能测试结果 ===")
        print(f"总请求数: {self.user_count}")
        print(f"成功数: {self.success_count}")
        print(f"失败数: {self.failure_count}")
        print(f"成功率: {self.success_count/self.user_count*100:.2f}%")
        print(f"总耗时: {end_time - start_time:.2f}s")
        print(f"QPS: {self.user_count/(end_time - start_time):.2f}")

if __name__ == "__main__":
    test = EnrollmentPerformanceTest("http://localhost:8080", user_count=1000)
    test.run_test(activity_id=1001, thread_count=50)
