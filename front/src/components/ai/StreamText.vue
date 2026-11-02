<script setup lang="ts">
/**
 * 流式正文视图：把 Markdown 正文渲染成排版良好的小说段落
 * 支持打字机光标、段落逐个淡入
 */
import { computed } from 'vue'

const props = withDefaults(
  defineProps<{
    content: string
    typing?: boolean
    minHeight?: number
    fontSize?: number
    lineHeight?: number
    serif?: boolean
  }>(),
  { minHeight: 320, fontSize: 15.5, lineHeight: 2.05 }
)

/** 极简 Markdown → HTML：支持 # 标题、粗体、引用、分隔线，正文按空行分段 */
const html = computed(() => {
  const src = props.content ?? ''
  if (!src.trim()) return ''
  const escape = (s: string) =>
    s.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')

  return src
    .split(/\n{2,}/)
    .map((block) => {
      const line = block.trim()
      if (!line) return ''
      if (/^#{1,6}\s/.test(line)) {
        const level = Math.min(6, line.match(/^#+/)![0].length)
        return `<h${level}>${escape(line.replace(/^#+\s/, ''))}</h${level}>`
      }
      if (/^(-{3,}|\*{3,})$/.test(line)) return '<hr/>'
      const isQuote = line.startsWith('>')
      const body = escape(line.replace(/^>\s?/, ''))
        .replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>')
        .replace(/\*([^*]+)\*/g, '<em>$1</em>')
        .replace(/`([^`]+)`/g, '<code>$1</code>')
        .replace(/\n/g, '<br/>')
      return isQuote ? `<blockquote>${body}</blockquote>` : `<p>${body}</p>`
    })
    .join('')
})
</script>

<template>
  <div
    class="reader"
    :class="{ serif, typing }"
    :style="{ minHeight: `${minHeight}px`, fontSize: `${fontSize}px`, lineHeight: lineHeight }"
  >
    <div v-if="html" class="md" v-html="html" />
    <p v-else class="ph">正文还没开始。确认章纲后点击「开始生成」，这里会逐句长出来。</p>
    <span v-if="typing" class="caret" aria-hidden="true" />
  </div>
</template>

<style scoped>
.reader {
  position: relative;
  color: #23263c;
  letter-spacing: 0.2px;
}

.reader.serif {
  font-family: var(--font-serif);
}

.md :deep(p) {
  margin: 0 0 1.05em;
  text-indent: 2em;
  animation: fade-in 500ms var(--ease-out) both;
}

.md :deep(p:first-child) {
  animation-delay: 40ms;
}

.md :deep(h1),
.md :deep(h2),
.md :deep(h3) {
  font-size: 1.22em;
  font-weight: 700;
  margin: 1.4em 0 0.7em;
  color: var(--text-1);
  padding-left: 12px;
  border-left: 4px solid var(--grad-brand);
  border-image: var(--grad-brand) 1;
  text-indent: 0;
  animation: fade-up 460ms var(--ease-out) both;
}

.md :deep(blockquote) {
  margin: 1em 0;
  padding: 10px 16px;
  border-radius: 10px;
  background: linear-gradient(90deg, rgba(99, 102, 241, 0.07), transparent);
  border-left: 3px solid var(--purple);
  color: var(--text-2);
  text-indent: 0;
  font-size: 0.94em;
}

.md :deep(hr) {
  margin: 1.6em 0;
  border: none;
  height: 1px;
  background: linear-gradient(90deg, transparent, var(--line-strong), transparent);
}

.md :deep(code) {
  font-family: var(--font-mono);
  font-size: 0.88em;
  padding: 1px 5px;
  border-radius: 5px;
  background: rgba(99, 102, 241, 0.1);
  color: var(--brand-strong);
}

.md :deep(strong) {
  font-weight: 700;
}

.caret {
  display: inline-block;
  width: 8px;
  height: 1.05em;
  margin-left: 2px;
  vertical-align: text-bottom;
  background: var(--grad-brand);
  border-radius: 2px;
  animation: caret-blink 1.05s steps(1) infinite;
}

.ph {
  color: var(--text-4);
  font-size: 13.5px;
  text-indent: 0;
  padding: 28px 4px;
}
</style>
