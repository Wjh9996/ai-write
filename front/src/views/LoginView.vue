<script setup lang="ts">
/** 登录页：左侧渐变说明区 + 右侧表单 */
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { useUiStore } from '@/stores/ui'
import AppButton from '@/components/common/AppButton.vue'
import AppInput from '@/components/common/AppInput.vue'
import AppIcon from '@/components/common/AppIcon.vue'

const router = useRouter()
const user = useUserStore()
const ui = useUiStore()

const form = reactive({ email: 'author@ai-write.dev', password: 'demo1234' })
const error = ref('')

const FEATURES = [
  { icon: 'layers', title: '三级大纲', desc: '总纲 → 卷纲事件 → 每章 3—7 个 beat' },
  { icon: 'think', title: '五段流式生成', desc: '单章约 2500 字，逐句长出，可随时停止' },
  { icon: 'shield', title: '设定不崩', desc: '摘要与设定变化需人工确认才回写上下文' }
]

async function submit() {
  error.value = ''
  if (!form.email.includes('@')) {
    error.value = '请填写正确的邮箱'
    return
  }
  if (form.password.length < 6) {
    error.value = '密码至少 6 位'
    return
  }
  try {
    await user.login(form)
    ui.success('登录成功', '欢迎回来，继续你的那一章')
    router.push('/')
  } catch (e) {
    error.value = (e as Error).message
  }
}
</script>

<template>
  <div class="login">
    <!-- 左侧 -->
    <section class="hero">
      <div class="blobs" aria-hidden="true">
        <span class="b b1" />
        <span class="b b2" />
        <span class="b b3" />
      </div>

      <div class="hero-in">
        <div class="logo">
          <span class="ico"><AppIcon name="write" :size="18" color="#fff" /></span>
          <div>
            <h1>长篇小说 AI 创作台</h1>
            <p>导入主线，AI 拆章纲，逐章写出可交付的正文</p>
          </div>
        </div>

        <ul class="feats">
          <li v-for="(f, i) in FEATURES" :key="f.title" :style="{ animationDelay: `${260 + i * 110}ms` }">
            <span class="fi"><AppIcon :name="f.icon" :size="16" /></span>
            <div>
              <b>{{ f.title }}</b>
              <small>{{ f.desc }}</small>
            </div>
          </li>
        </ul>

        <div class="quote">
          <p>“每一章约 2500 字，把设定钉死，剩下的交给时间。”</p>
        </div>
      </div>
    </section>

    <!-- 右侧 -->
    <section class="panel">
      <form class="form" @submit.prevent="submit">
        <h2>登录创作台</h2>
        <p class="sub">个人 MVP 版，单用户，数据保存在本机</p>

        <AppInput v-model="form.email" label="邮箱" placeholder="you@example.com" />
        <AppInput v-model="form.password" label="密码" type="password" placeholder="至少 6 位" />

        <p v-if="error" class="err animate-shake">{{ error }}</p>

        <AppButton
          variant="primary"
          size="lg"
          block
          icon="arrowRight"
          :loading="user.loading"
          @click="submit"
        >
          进入创作台
        </AppButton>

        <div class="tip">
          <AppIcon name="info" :size="14" />
          <span>当前为 Mock 模式，任意合规邮箱即可进入。后端就绪后在 <code>.env</code> 里关闭即可切换。</span>
        </div>
      </form>
    </section>
  </div>
</template>

<style scoped>
.login {
  min-height: 100%;
  display: grid;
  grid-template-columns: 1.15fr 0.85fr;
}

/* 左 */
.hero {
  position: relative;
  overflow: hidden;
  padding: 54px 56px;
  display: flex;
  align-items: center;
  background: var(--grad-hero-bg), var(--bg);
}

.blobs {
  position: absolute;
  inset: 0;
  pointer-events: none;
}
.b {
  position: absolute;
  border-radius: 50%;
  filter: blur(58px);
  opacity: 0.5;
}
.b1 {
  width: 320px;
  height: 320px;
  background: linear-gradient(135deg, #818cf8, #c084fc);
  top: -60px;
  left: -40px;
  animation: roam 18s var(--ease-in-out) infinite;
}
.b2 {
  width: 280px;
  height: 280px;
  background: linear-gradient(135deg, #f472b6, #fb923c);
  bottom: -80px;
  left: 40%;
  animation: roam 22s var(--ease-in-out) infinite reverse;
}
.b3 {
  width: 240px;
  height: 240px;
  background: linear-gradient(135deg, #22d3ee, #818cf8);
  top: 40%;
  right: -60px;
  animation: roam 26s var(--ease-in-out) infinite;
}

.hero-in {
  position: relative;
  max-width: 520px;
  animation: fade-up 700ms var(--ease-out) both;
}

.logo {
  display: flex;
  gap: 14px;
  align-items: center;
}
.logo .ico {
  width: 46px;
  height: 46px;
  border-radius: 14px;
  display: grid;
  place-items: center;
  background: var(--grad-brand);
  box-shadow: var(--sh-brand);
  animation: float-y 5s var(--ease-in-out) infinite;
  flex: none;
}
.logo h1 {
  font-size: 25px;
  font-weight: 800;
  letter-spacing: -0.4px;
}
.logo p {
  font-size: 13px;
  color: var(--text-3);
  margin-top: 2px;
}

.feats {
  margin-top: 42px;
  display: flex;
  flex-direction: column;
  gap: 14px;
}
.feats li {
  display: flex;
  gap: 12px;
  align-items: flex-start;
  padding: 13px 15px;
  border-radius: var(--r-lg);
  background: rgba(255, 255, 255, 0.72);
  border: 1px solid rgba(255, 255, 255, 0.9);
  box-shadow: var(--sh-sm);
  backdrop-filter: blur(8px);
  animation: fade-up 700ms var(--ease-out) both;
  transition: transform var(--dur-base) var(--ease-out);
}
.feats li:hover {
  transform: translateY(-3px) translateX(3px);
}
.fi {
  width: 34px;
  height: 34px;
  border-radius: 11px;
  display: grid;
  place-items: center;
  color: #fff;
  background: var(--grad-brand);
  flex: none;
}
.feats li:nth-child(2) .fi {
  background: var(--grad-ocean);
}
.feats li:nth-child(3) .fi {
  background: var(--grad-mint);
}
.feats b {
  font-size: 14px;
}
.feats small {
  display: block;
  font-size: 12px;
  color: var(--text-3);
  margin-top: 1px;
}

.quote {
  margin-top: 34px;
  padding: 14px 18px;
  border-left: 3px solid var(--pink);
  border-radius: 0 var(--r-md) var(--r-md) 0;
  background: rgba(255, 255, 255, 0.6);
  animation: fade-in 900ms var(--ease-out) 600ms both;
}
.quote p {
  font-size: 13px;
  color: var(--text-2);
  font-family: var(--font-serif);
}

/* 右 */
.panel {
  display: grid;
  place-items: center;
  padding: 40px;
  background: var(--surface);
  border-left: 1px solid var(--line);
}

.form {
  width: 100%;
  max-width: 340px;
  display: flex;
  flex-direction: column;
  gap: 16px;
  animation: fade-up 700ms var(--ease-out) 120ms both;
}
.form h2 {
  font-size: 21px;
  font-weight: 800;
}
.sub {
  font-size: 12.5px;
  color: var(--text-3);
  margin-top: -10px;
}

.err {
  font-size: 12.5px;
  color: var(--c-danger);
  background: rgba(239, 68, 68, 0.08);
  padding: 8px 11px;
  border-radius: var(--r-sm);
}

.tip {
  display: flex;
  gap: 8px;
  align-items: flex-start;
  font-size: 11.5px;
  color: var(--text-3);
  background: var(--surface-3);
  padding: 10px 12px;
  border-radius: var(--r-md);
  line-height: 1.6;
}
.tip code {
  font-family: var(--font-mono);
  background: rgba(99, 102, 241, 0.1);
  color: var(--brand-strong);
  padding: 1px 4px;
  border-radius: 4px;
  font-size: 11px;
}
.tip svg {
  flex: none;
  margin-top: 2px;
}

@keyframes shake {
  0%,
  100% {
    transform: translateX(0);
  }
  25% {
    transform: translateX(-5px);
  }
  75% {
    transform: translateX(5px);
  }
}
.animate-shake {
  animation: shake 320ms var(--ease-out);
}

@media (max-width: 900px) {
  .login {
    grid-template-columns: 1fr;
  }
  .hero {
    display: none;
  }
}
</style>
