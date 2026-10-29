<script setup lang="ts">
defineProps<{ eyebrow?: string; title: string; description?: string; to?: string; action?: string }>()
</script>

<template>
  <div class="section-head">
    <div>
      <span v-if="eyebrow" class="eyebrow">{{ eyebrow }}</span>
      <h2>{{ title }}</h2>
      <p v-if="description">{{ description }}</p>
    </div>
    <NuxtLink v-if="to && action" class="text-link" :to="to">{{ action }} →</NuxtLink>
  </div>
</template>
