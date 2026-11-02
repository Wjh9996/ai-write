package com.wjh.aiwrite.support;

import com.wjh.aiwrite.common.BizException;
import com.wjh.aiwrite.config.CurrentUser;
import com.wjh.aiwrite.novel.model.Novel;
import com.wjh.aiwrite.novel.repository.NovelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 所有权校验（不可妥协规则⑧：所有写入按 user_id + novel_id 做权限校验）
 *
 * 所有 controller 都通过它拿 Novel，避免每个接口重复写校验代码。
 */
@Service
@RequiredArgsConstructor
public class Ownership {

    private final CurrentUser currentUser;
    private final NovelRepository novelRepository;

    /** 取当前用户 id */
    public Long userId() {
        return currentUser.id();
    }

    /** 校验并取作品，不属于当前用户直接 403 */
    public Novel requireNovel(Long novelId) {
        Novel novel = novelRepository.findById(novelId)
                .orElseThrow(() -> BizException.notFound("作品不存在"));
        if (!novel.getUserId().equals(userId())) {
            throw BizException.forbidden("无权访问该作品");
        }
        return novel;
    }
}
