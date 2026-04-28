<script>
  export let text = "";
</script>

{#if text}
  <button class="info-tip" type="button" aria-label={text}>
    i
    <span class="info-bubble" role="tooltip">
      {text}
    </span>
  </button>
{/if}
