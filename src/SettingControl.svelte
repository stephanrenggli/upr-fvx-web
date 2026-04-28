<script>
  import InfoTip from "./InfoTip.svelte";
  import { labelize, normalizeValue } from "./settingsBuilder.js";

  export let setting;
  export let value;
  export let onChange;

  $: label = setting.label || labelize(setting.name);

  function updateFromInput(event) {
    onChange(setting.type === "integer" ? normalizeValue(event.target.value, setting) : event.target.value);
  }
</script>

{#if setting.type === "boolean"}
  <label class="check compact">
    <input type="checkbox" checked={Boolean(value)} on:change={(event) => onChange(event.target.checked)} />
    <span>
      {label}
      <InfoTip text={setting.tooltip} />
    </span>
  </label>
{:else if setting.type === "enum"}
  <label class="compact-field">
    <span>
      {label}
      <InfoTip text={setting.tooltip} />
    </span>
    <select value={value} on:change={updateFromInput}>
      {#each setting.values as option}
        <option value={option}>{labelize(option)}</option>
      {/each}
    </select>
  </label>
{:else}
  <label class="compact-field">
    <span>
      {label}
      <InfoTip text={setting.tooltip} />
    </span>
    <input
      type={setting.type === "integer" ? "number" : "text"}
      value={value}
      on:input={updateFromInput}
    />
  </label>
{/if}
