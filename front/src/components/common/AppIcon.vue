<script setup lang="ts">
/**
 * 内联 SVG 图标集 —— 避免图标库依赖，统一 24 视口、currentColor 描边
 */
const props = withDefaults(
  defineProps<{
    name: string
    size?: number | string
    strokeWidth?: number
    color?: string
  }>(),
  { size: 18, strokeWidth: 1.8, color: 'currentColor' }
)

const PATHS: Record<string, string> = {
  outline: 'M5 4h9l5 5v11a1 1 0 0 1-1 1H5a1 1 0 0 1-1-1V5a1 1 0 0 1 1-1Z|M13 4v5h5|M8 13h8|M8 17h5',
  write: 'M4 20h4l10-10a2.8 2.8 0 0 0-4-4L4 16v4Z|M13.5 6.5l4 4',
  settings: 'M12 15.5a3.5 3.5 0 1 0 0-7 3.5 3.5 0 0 0 0 7Z|M19.4 15a1.7 1.7 0 0 0 .3 1.9l.1.1a2 2 0 1 1-2.8 2.8l-.1-.1a1.7 1.7 0 0 0-2.9 1.2v.2a2 2 0 1 1-4 0v-.1a1.7 1.7 0 0 0-1.1-1.6 1.7 1.7 0 0 0-1.9.4l-.1.1a2 2 0 1 1-2.8-2.8l.1-.1a1.7 1.7 0 0 0-1.2-2.9H3a2 2 0 1 1 0-4h.2a1.7 1.7 0 0 0 1.5-1.1 1.7 1.7 0 0 0-.4-1.9l-.1-.1a2 2 0 1 1 2.8-2.8l.1.1a1.7 1.7 0 0 0 1.9.3H10a1.7 1.7 0 0 0 1-1.5V3a2 2 0 1 1 4 0v.2a1.7 1.7 0 0 0 2.9 1.2l.1-.1a2 2 0 1 1 2.8 2.8l-.1.1a1.7 1.7 0 0 0-.3 1.9V10a1.7 1.7 0 0 0 1.5 1H21a2 2 0 1 1 0 4h-.2a1.7 1.7 0 0 0-1.5 1Z',
  export: 'M12 3v12|M8 11l4 4 4-4|M4 17v2a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2v-2',
  usage: 'M4 20V10|M10 20V4|M16 20v-7|M22 20H2',
  review: 'M9 20H5a2 2 0 0 1-2-2V9l7-5 7 5v7a2 2 0 0 1-2 2h-4|M9 20v-6h6v6|M14 12 9 9l7-5V3l-7 5-3-2',
  plus: 'M12 5v14|M5 12h14',
  close: 'M6 6l12 12|M18 6L6 18',
  check: 'M4 12.5l5 5L20 6.5',
  edit: 'M4 20h4L18.5 9.5a2.1 2.1 0 0 0-3-3L5 17v3Z|M14 6l4 4',
  trash: 'M4 7h16|M9 7V5a1 1 0 0 1 1-1h4a1 1 0 0 1 1 1v2|M6 7l1 13a1 1 0 0 0 1 1h8a1 1 0 0 0 1-1l1-13|M10 11v6|M14 11v6',
  sparkle: 'M12 3l1.6 4.4L18 9l-4.4 1.6L12 15l-1.6-4.4L6 9l4.4-1.6L12 3Z|M18.5 15l.7 1.8 1.8.7-1.8.7-.7 1.8-.7-1.8-1.8-.7 1.8-.7.7-1.8Z',
  play: 'M7 4.5v15l12-7.5-12-7.5Z',
  pause: 'M8 5v14|M16 5v14',
  stop: 'M6.5 6.5h11v11h-11z',
  refresh: 'M20 11a8 8 0 0 0-14-4.5L4 8.5|M4 4v4.5h4.5|M4 13a8 8 0 0 0 14 4.5L20 15.5|M20 20v-4.5h-4.5',
  book: 'M4 5.5C4 4.7 4.7 4 5.5 4H10a2 2 0 0 1 2 2v13a2 2 0 0 0-2-2H5.5A1.5 1.5 0 0 1 4 15.5v-10Z|M20 5.5c0-.8-.7-1.5-1.5-1.5H14a2 2 0 0 0-2 2v13a2 2 0 0 1 2-2h4.5a1.5 1.5 0 0 0 1.5-1.5v-10Z',
  user: 'M12 12a4 4 0 1 0 0-8 4 4 0 0 0 0 8Z|M4 20a8 8 0 0 1 16 0',
  users: 'M9 11a3.5 3.5 0 1 0 0-7 3.5 3.5 0 0 0 0 7Z|M2.5 19a6.5 6.5 0 0 1 13 0|M16.5 5.2a3.5 3.5 0 0 1 0 6.6|M18 13.4a6.5 6.5 0 0 1 3.5 5.6',
  globe: 'M12 21a9 9 0 1 0 0-18 9 9 0 0 0 0 18Z|M3 12h18|M12 3a14 14 0 0 1 0 18 14 14 0 0 1 0-18Z',
  clock: 'M12 21a9 9 0 1 0 0-18 9 9 0 0 0 0 18Z|M12 7.5V12l3 2',
  link: 'M10 13a4 4 0 0 0 5.7.4l2.6-2.6a4 4 0 0 0-5.7-5.7L11.5 6.4|M14 11a4 4 0 0 0-5.7-.4l-2.6 2.6a4 4 0 0 0 5.7 5.7l1.1-1.1',
  palette: 'M12 21a9 9 0 1 1 9-9c0 2-1.6 3-3.5 3H16a2 2 0 0 0-1.4 3.4A1.7 1.7 0 0 1 12 21Z|M7.5 11.5h.01|M11 8.5h.01|M15.5 9.5h.01',
  arrowRight: 'M5 12h14|M13 6l6 6-6 6',
  arrowLeft: 'M19 12H5|M11 6l-6 6 6 6',
  chevronDown: 'M6 9l6 6 6-6',
  chevronRight: 'M9 6l6 6-6 6',
  chevronLeft: 'M15 6l-6 6 6 6',
  search: 'M11 18a7 7 0 1 0 0-14 7 7 0 0 0 0 14Z|M16.5 16.5 21 21',
  logout: 'M15 4h3a2 2 0 0 1 2 2v12a2 2 0 0 1-2 2h-3|M10 8l-4 4 4 4|M6 12h10',
  folder: 'M3 7a2 2 0 0 1 2-2h4l2 2.5h8a2 2 0 0 1 2 2V17a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V7Z',
  file: 'M6 3h7l5 5v13a1 1 0 0 1-1 1H6a1 1 0 0 1-1-1V4a1 1 0 0 1 1-1Z|M13 3v5h5',
  upload: 'M12 16V4|M7.5 8.5 12 4l4.5 4.5|M4 16v3a1 1 0 0 0 1 1h14a1 1 0 0 0 1-1v-3',
  download: 'M12 4v12|M7.5 11.5 12 16l4.5-4.5|M4 16v3a1 1 0 0 0 1 1h14a1 1 0 0 0 1-1v-3',
  drag: 'M9 6h.01|M9 12h.01|M9 18h.01|M15 6h.01|M15 12h.01|M15 18h.01',
  filter: 'M4 5h16l-6 7v6l-4 2v-8L4 5Z',
  bolt: 'M13 3 5 14h6l-1 7 8-11h-6l1-7Z',
  shield: 'M12 3l7 3v6c0 4-3 7.5-7 9-4-1.5-7-5-7-9V6l7-3Z|M9.5 12l1.8 1.8L15 10',
  warning: 'M12 4l9 16H3l9-16Z|M12 10v4|M12 17h.01',
  info: 'M12 21a9 9 0 1 0 0-18 9 9 0 0 0 0 18Z|M12 11v5|M12 8h.01',
  eye: 'M2.5 12S6 6 12 6s9.5 6 9.5 6-3.5 6-9.5 6-9.5-6-9.5-6Z|M12 15a3 3 0 1 0 0-6 3 3 0 0 0 0 6Z',
  layers: 'M12 3l9 5-9 5-9-5 9-5Z|M3 13l9 5 9-5|M3 17.5l9 5 9-5',
  clipboard: 'M9 4h6v3H9V4Z|M8 5H6a1 1 0 0 0-1 1v13a1 1 0 0 0 1 1h12a1 1 0 0 0 1-1V6a1 1 0 0 0-1-1h-2|M8 11h8|M8 15h5',
  think: 'M9 18h6|M10 21h4|M12 4a6 6 0 0 1 4 10.5c-.6.5-1 1.2-1 2H9c0-.8-.4-1.5-1-2A6 6 0 0 1 12 4Z',
  target: 'M12 21a9 9 0 1 0 0-18 9 9 0 0 0 0 18Z|M12 16.5a4.5 4.5 0 1 0 0-9 4.5 4.5 0 0 0 0 9Z|M12 13.5a1.5 1.5 0 1 0 0-3 1.5 1.5 0 0 0 0 3Z',
  coin: 'M12 21c4.4 0 8-1.6 8-3.5V9.5C20 7.6 16.4 6 12 6S4 7.6 4 9.5v8C4 19.4 7.6 21 12 21Z|M20 9.5c0 1.9-3.6 3.5-8 3.5s-8-1.6-8-3.5|M4 14c0 1.9 3.6 3.5 8 3.5s8-1.6 8-3.5',
  spinner: 'M12 3a9 9 0 1 0 9 9'
}

const group = PATHS[props.name] ?? PATHS.info
const paths = group.split('|')
</script>

<template>
  <svg
    class="app-icon"
    :width="size"
    :height="size"
    viewBox="0 0 24 24"
    :fill="props.name === 'play' ? color : 'none'"
    :stroke="color"
    :stroke-width="strokeWidth"
    stroke-linecap="round"
    stroke-linejoin="round"
    aria-hidden="true"
  >
    <path v-for="(d, i) in paths" :key="i" :d="d" />
  </svg>
</template>

<style scoped>
.app-icon {
  display: block;
  flex: none;
}
</style>
