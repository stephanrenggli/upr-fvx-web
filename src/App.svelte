<script>
  import { onDestroy, onMount } from "svelte";
  import SettingControl from "./SettingControl.svelte";
  import {
    countGroupSettings,
    createInitialSettings,
    defaultValueFor,
    getGroupedSettings,
    toBridgePayload
  } from "./settingsBuilder.js";

  let rom = null;
  let settingsMode = "builder";
  let settingsSchema = null;
  let builderSettings = {};
  let settingsString = "";
  let seed = "";
  let saveLog = false;
  let status = "idle";
  let message = "";
  let generatedSettingsString = "";
  let settingsError = "";
  let activeGroup = "General";
  let encodeController = null;
  let encodeTimeout = null;

  $: activeSettingsString = settingsMode === "builder" ? generatedSettingsString : settingsString.trim();
  $: canSubmit = rom && activeSettingsString && status !== "running";
  $: groups = groupedSettings(settingsSchema);
  $: selectedGroup = groups.find(([title]) => title === activeGroup) ?? groups[0];
  $: if (settingsSchema) {
    scheduleSettingsEncode(settingsSchema, builderSettings);
  }

  onMount(() => {
    const controller = new AbortController();
    loadSchema(controller.signal);
    return () => controller.abort();
  });

  onDestroy(() => {
    clearTimeout(encodeTimeout);
    encodeController?.abort();
  });

  async function loadSchema(signal) {
    try {
      const response = await fetch("/api/settings/schema", { signal });
      if (!response.ok) {
        const payload = await response.json().catch(() => ({}));
        throw new Error(payload.error || `Settings schema failed with status ${response.status}`);
      }
      const schema = await response.json();
      settingsSchema = schema;
      builderSettings = createInitialSettings(schema);
    } catch (error) {
      if (error.name !== "AbortError") {
        settingsError = error.message;
      }
    }
  }

  function scheduleSettingsEncode(schema, settings) {
    clearTimeout(encodeTimeout);
    encodeController?.abort();
    encodeController = new AbortController();

    encodeTimeout = setTimeout(async () => {
      try {
        settingsError = "";
        const response = await fetch("/api/settings/encode", {
          method: "POST",
          headers: {
            "Content-Type": "application/json"
          },
          body: JSON.stringify(toBridgePayload(settings, schema)),
          signal: encodeController.signal
        });

        if (!response.ok) {
          const payload = await response.json().catch(() => ({}));
          throw new Error(payload.error || `Settings encode failed with status ${response.status}`);
        }

        const payload = await response.json();
        generatedSettingsString = payload.settingsString;
      } catch (error) {
        if (error.name !== "AbortError") {
          generatedSettingsString = "";
          settingsError = error.message;
        }
      }
    }, 150);
  }

  function groupedSettings(schema) {
    if (!schema) {
      return [];
    }

    try {
      settingsError = "";
      return getGroupedSettings(schema);
    } catch (error) {
      settingsError = error.message;
      return [];
    }
  }

  function updateSetting(key, value) {
    builderSettings = { ...builderSettings, [key]: value };
  }

  function resetSettings() {
    if (settingsSchema) {
      builderSettings = createInitialSettings(settingsSchema);
    }
  }

  async function handleSubmit() {
    if (!canSubmit) {
      return;
    }

    status = "running";
    message = "Uploading ROM and running FVX. Large games can take a while.";

    const form = new FormData();
    form.append("rom", rom);
    form.append("settingsString", activeSettingsString);
    form.append("saveLog", String(saveLog));
    if (seed.trim()) {
      form.append("seed", seed.trim());
    }

    try {
      const response = await fetch("/api/randomize", {
        method: "POST",
        body: form
      });

      if (!response.ok) {
        const payload = await response.json().catch(() => ({}));
        throw new Error(payload.error || `Request failed with status ${response.status}`);
      }

      const blob = await response.blob();
      const filename = parseDownloadName(response.headers.get("content-disposition")) || defaultDownloadName(rom.name, saveLog);
      downloadBlob(blob, filename);
      status = "done";
      message = `Downloaded ${filename}.`;
    } catch (error) {
      status = "error";
      message = error.message;
    }
  }

  function generateSeed() {
    const nextSeed = crypto.getRandomValues(new BigUint64Array(1))[0] & BigInt("0x7fffffffffffffff");
    seed = nextSeed.toString();
  }

  function parseDownloadName(disposition) {
    const match = disposition?.match(/filename="([^"]+)"/i);
    return match?.[1];
  }

  function defaultDownloadName(originalName, includeLog) {
    if (includeLog) {
      return "upr-fvx-result.zip";
    }
    const dot = originalName.lastIndexOf(".");
    if (dot === -1) {
      return `${originalName}-randomized`;
    }
    return `${originalName.slice(0, dot)}-randomized${originalName.slice(dot)}`;
  }

  function downloadBlob(blob, filename) {
    const url = URL.createObjectURL(blob);
    const link = document.createElement("a");
    link.href = url;
    link.download = filename;
    document.body.appendChild(link);
    link.click();
    link.remove();
    URL.revokeObjectURL(url);
  }
</script>

<main class="shell">
  <section class="hero">
    <div class="hero-copy">
    <p class="eyebrow">UPR-FVX Web</p>
      <h1>Randomize a ROM from your browser.</h1>
      <p class="lede">
      Build FVX settings visually, upload your ROM, and download a randomized game without opening the desktop app.
      Files are handled per request and removed after processing.
      </p>
    </div>
    <div class="hero-orb" aria-hidden="true">
      <span></span>
    </div>
  </section>

  <form class="panel" on:submit|preventDefault={handleSubmit}>
    <label class="field">
      <span>ROM file</span>
      <input
        type="file"
        accept=".gb,.sgb,.gbc,.gba,.nds,.3ds,.cci,.cxi"
        on:change={(event) => rom = event.target.files?.[0] ?? null}
      />
    </label>

    <div class="mode-tabs" role="tablist" aria-label="Settings input mode">
      <button
        class={settingsMode === "builder" ? "tab active" : "tab"}
        type="button"
        on:click={() => settingsMode = "builder"}
      >
        Visual builder
      </button>
      <button
        class={settingsMode === "manual" ? "tab active" : "tab"}
        type="button"
        on:click={() => settingsMode = "manual"}
      >
        Paste string
      </button>
    </div>

    {#if settingsMode === "builder"}
      <section class="builder">
        {#if !settingsSchema}
          <p class="builder-note">Loading FVX settings schema from the bridge...</p>
          {#if settingsError}
            <p class="status error">{settingsError}</p>
          {/if}
        {:else}
          {#if selectedGroup}
            <div class="settings-layout">
              <nav class="category-nav" aria-label="Settings categories">
                {#each groups as [title, sections]}
                  <button
                    class={selectedGroup[0] === title ? "category-nav-button active" : "category-nav-button"}
                    type="button"
                    aria-current={selectedGroup[0] === title ? "page" : undefined}
                    on:click={() => activeGroup = title}
                  >
                    <span>{title}</span>
                    <strong>{countGroupSettings(sections)}</strong>
                  </button>
                {/each}
              </nav>

              <fieldset class="settings-tab-panel">
                <legend>{selectedGroup[0]}</legend>
                <div class="settings-sections">
                  {#each selectedGroup[1] as [sectionTitle, sectionSettings]}
                    <section class="settings-section">
                      <h3>{sectionTitle}</h3>
                      <div class="tab-panel-grid">
                        {#each sectionSettings as setting (setting.name)}
                          <SettingControl
                            {setting}
                            value={builderSettings[setting.name] ?? defaultValueFor(setting)}
                            onChange={(value) => updateSetting(setting.name, value)}
                          />
                        {/each}
                      </div>
                    </section>
                  {/each}
                </div>
              </fieldset>
            </div>
          {/if}

          <div class="builder-footer">
            <button class="reset-button" type="button" on:click={resetSettings}>
              Reset settings
            </button>
            <details class="settings-string-details">
              <summary>Generated settings string</summary>
              <label class="field">
                <span>FVX settings string used for this run</span>
                <textarea rows="4" readonly>{generatedSettingsString || "Waiting for FVX settings bridge..."}</textarea>
              </label>
            </details>
          </div>

          {#if settingsError}
            <p class="status error">{settingsError}</p>
          {/if}
        {/if}
      </section>
    {:else}
      <label class="field">
        <span>FVX settings string</span>
        <textarea
          rows="8"
          placeholder="Paste the settings string generated by UPR-FVX."
          bind:value={settingsString}
        ></textarea>
      </label>
    {/if}

    <div class="seed-row">
      <label class="field">
        <span>Seed</span>
        <input
          inputmode="numeric"
          placeholder="Optional. Leave blank for FVX default."
          bind:value={seed}
        />
      </label>
      <button class="secondary" type="button" on:click={generateSeed}>
        Generate seed
      </button>
    </div>

    <label class="check">
      <input type="checkbox" bind:checked={saveLog} />
      <span>Include detailed log as a zip download</span>
    </label>

    <button class="primary" type="submit" disabled={!canSubmit}>
      {status === "running" ? "Randomizing..." : "Randomize ROM"}
    </button>

    {#if message}
      <p class={`status ${status}`}>{message}</p>
    {/if}
  </form>

</main>
