<script setup lang="ts">
/** 用量与成本 */
import { computed, onMounted, ref } from 'vue'
import { useUiStore } from '@/stores/ui'
import { usageApi } from '@/api'
import type { UsageOverview } from '@/types'
import StatCard from '@/components/common/StatCard.vue'
import AppCard from '@/components/common/AppCard.vue'
import AppTag from '@/components/common/AppTag.vue'
import AppIcon from '@/components/common/AppIcon.vue'
import AppProgress from '@/components/common/AppProgress.vue'
import AppSkeleton from '@/components/common/AppSkeleton.vue'

const ui = useUiStore()
const data = ref<UsageOverview | null>(null)

onMounted(async () => {
  try {
    data.value = await usageApi.overview()
  } catch (e) {
    ui.error('用量加载失败', (e as Error).message)
  }
})

const TONES = ['99,102,241', '236,72,153', '56,189,248', '245,158,11', '34,197,94', '168,85,247']

const scenes = computed(() => data.value?.byScene ?? [])
const maxScene = computed(() => Math.max(1, ...scenes.value.map((s) => s.cost)))
const budgetPct = computed(() =>
  data.value ? Math.round((data.value.monthCost / data.value.budget) * 100) : 0
)
const avgPerChapter = computed(() =>
  data.value && data.value.chapterThisMonth
    ? Number((data.value.monthCost / data.value.chapterThisMonth).toFixed(3))
    : 0
)
</script>

<template>
  <div class="page">
    <header class="hd">
      <div>
        <h2>用量与成本</h2>
        <p>预算目标每月 50—100 元。这里的费用是估算值，以模型厂商实际计费为准。</p>
      </div>
      <AppTag label="按实际 tokens 记录" tone="teal" icon="info" />
    </header>

    <section v-if="!data" class="stats"><AppCard v-for="i in 4" :key="i" flat><AppSkeleton :lines="3" /></AppCard></section>

    <template v-else>
      <section class="stats">
        <StatCard
          label="本月花费"
          :value="Number(data.monthCost.toFixed(2))"
          suffix="元"
          icon="coin"
          tone="brand"
          :progress="budgetPct"
          :hint="`预算 ${data.budget} 元 · 已用 ${budgetPct}%`"
        />
        <StatCard
          label="Token 总量"
          :value="Number((data.monthTokens / 1000).toFixed(1))"
          suffix="k"
          icon="bolt"
          tone="violet"
          hint="输入预估 6k×5 + 输出 0.8k×5 / 章"
        />
        <StatCard
          label="本月生成章节"
          :value="data.chapterThisMonth"
          suffix="章"
          icon="book"
          tone="mint"
          hint="含重生成与自检"
        />
        <StatCard
          label="单章均价"
          :value="avgPerChapter"
          suffix="元"
          icon="target"
          tone="sunset"
          hint="目标区间 0.08—0.2 元"
        />
      </section>

      <AppCard title="按场景分布" desc="哪个环节最烧钱，一眼看出来" icon="usage" tone="amber">
        <ul class="bars">
          <li v-for="(s, i) in scenes" :key="s.scene" :style="{ animationDelay: `${i * 70}ms` }">
            <span class="sname">{{ s.scene }}</span>
            <div class="strack">
              <i
                :style="{
                  width: `${(s.cost / maxScene) * 100}%`,
                  background: `linear-gradient(90deg, rgba(${TONES[i % TONES.length]},.75), rgb(${TONES[i % TONES.length]}))`
                }"
              />
            </div>
            <b class="mono">¥{{ s.cost.toFixed(2) }}</b>
            <em class="mono">{{ s.times }} 次</em>
          </li>
        </ul>
      </AppCard>

      <AppCard title="调用明细" desc="每次调用都记录了模型、tokens 与估算费用" icon="clipboard" tone="ocean">
        <div class="tbl-wrap">
          <table class="tbl">
            <thead>
              <tr>
                <th>时间</th>
                <th>场景</th>
                <th>模型</th>
                <th class="r">输入</th>
                <th class="r">输出</th>
                <th class="r">费用</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(r, i) in data.records.slice(0, 14)" :key="r.id" :style="{ animationDelay: `${i * 30}ms` }">
                <td class="mono dim">{{ new Date(r.createdAt).toLocaleString('zh-CN').slice(5, 16) }}</td>
                <td>{{ r.scene }}</td>
                <td class="mono dim">{{ r.model }}</td>
                <td class="r mono">{{ r.inputTokens.toLocaleString('zh-CN') }}</td>
                <td class="r mono">{{ r.outputTokens.toLocaleString('zh-CN') }}</td>
                <td class="r mono cost">¥{{ r.estimatedCost.toFixed(2) }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </AppCard>

      <AppCard title="预算护栏" icon="shield" tone="mint">
        <div class="guard">
          <AppIcon name="info" :size="15" />
          <div>
            <b>月预算 ¥{{ data.budget }}</b>
            <AppProgress :value="budgetPct" :height="8" striped :tone="budgetPct > 80 ? '244,63,94' : '34,197,94'" />
            <small>
              {{
                budgetPct > 80
                  ? '已接近预算上限，建议减少重生成次数或改用更便宜的模型档位'
                  : '当前用量健康。每次模型调用都会自动记账，超阈值会提示。'
              }}
            </small>
          </div>
        </div>
      </AppCard>
    </template>
  </div>
</template>

<style scoped>
.page {
  padding: 22px 24px 60px;
  display: flex;
  flex-direction: column;
  gap: 16px;
  max-width: 1120px;
}

.hd {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 14px;
  animation: fade-up 400ms var(--ease-out) both;
}
.hd h2 {
  font-size: 20px;
  font-weight: 800;
}
.hd p {
  font-size: 12.5px;
  color: var(--text-3);
  margin-top: 2px;
}

.stats {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 14px;
}

.bars {
  display: flex;
  flex-direction: column;
  gap: 13px;
}
.bars li {
  display: grid;
  grid-template-columns: 90px 1fr 74px 54px;
  gap: 12px;
  align-items: center;
  animation: fade-up var(--dur-slow) var(--ease-out) both;
}
.sname {
  font-size: 12.5px;
  font-weight: 600;
  color: var(--text-2);
}
.strack {
  height: 9px;
  border-radius: var(--r-full);
  background: var(--surface-3);
  overflow: hidden;
}
.strack i {
  display: block;
  height: 100%;
  border-radius: inherit;
  transition: width 800ms var(--ease-spring);
  animation: none;
}
.bars b {
  text-align: right;
  font-size: 12.5px;
  color: var(--text-1);
}
.bars em {
  text-align: right;
  font-size: 11.5px;
  color: var(--text-4);
  font-style: normal;
}

.tbl-wrap {
  overflow: auto;
  border-radius: var(--r-md);
  border: 1px solid var(--line);
}
.tbl {
  width: 100%;
  border-collapse: collapse;
  font-size: 12.5px;
}
.tbl th {
  text-align: left;
  padding: 10px 12px;
  background: var(--surface-3);
  color: var(--text-3);
  font-weight: 600;
  font-size: 11.5px;
  white-space: nowrap;
  position: sticky;
  top: 0;
}
.tbl td {
  padding: 9px 12px;
  border-top: 1px solid var(--line);
  color: var(--text-2);
  white-space: nowrap;
}
.tbl tbody tr {
  animation: fade-in 380ms var(--ease-out) both;
  transition: background var(--dur-fast) linear;
}
.tbl tbody tr:hover {
  background: var(--surface-3);
}
.r {
  text-align: right;
}
.dim {
  color: var(--text-4);
}
.cost {
  color: var(--c-success);
  font-weight: 700;
}

.guard {
  display: flex;
  gap: 11px;
  align-items: flex-start;
}
.guard svg {
  flex: none;
  margin-top: 3px;
  color: var(--purple);
}
.guard b {
  font-size: 13px;
}
.guard small {
  display: block;
  font-size: 11.8px;
  color: var(--text-3);
  margin-top: 6px;
  line-height: 1.6;
}

@media (max-width: 640px) {
  .bars li {
    grid-template-columns: 70px 1fr 60px;
  }
  .bars em {
    display: none;
  }
}
</style>
