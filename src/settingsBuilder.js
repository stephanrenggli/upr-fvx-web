export const defaultBuilderSettings = {
  allowWonderGuard: true,
  blockWildLegendaries: true,
  randomizeIntroMon: true,
  selectedEXPCurve: "MEDIUM_FAST",
  trainersBlockEarlyWonderGuard: true,
  trainersBlockLegendaries: true
};

export function createInitialSettings(schema) {
  return Object.fromEntries(
    schema.settings
      .map((setting) => [setting.name, defaultValueFor(setting)])
  );
}

export function toBridgePayload(settings, schema) {
  const payload = {};

  for (const setting of schema.settings) {
    if (!(setting.name in settings)) {
      continue;
    }

    const value = normalizeValue(settings[setting.name], setting);
    const defaultValue = defaultValueFor(setting);
    if (value !== defaultValue) {
      payload[setting.name] = value;
    }
  }

  return payload;
}

export function defaultValueFor(setting) {
  if (setting.name in defaultBuilderSettings) {
    return defaultBuilderSettings[setting.name];
  }
  if (setting.type === "boolean") {
    return false;
  }
  if (setting.type === "integer") {
    return 0;
  }
  if (setting.type === "string") {
    return "";
  }
  return setting.values?.[0] ?? "";
}

export function normalizeValue(value, setting) {
  if (setting.type === "integer") {
    return Number.parseInt(value, 10) || 0;
  }
  return value;
}

export function getGroupedSettings(schema) {
  const groups = new Map();
  for (const setting of schema.settings) {
    if (!setting.group || !setting.section) {
      throw new Error(`Settings schema entry ${setting.name} is missing group/section metadata.`);
    }

    const groupName = setting.group;
    const sectionName = setting.section;

    if (!groups.has(groupName)) {
      groups.set(groupName, new Map());
    }
    const sections = groups.get(groupName);
    if (!sections.has(sectionName)) {
      sections.set(sectionName, []);
    }
    sections.get(sectionName).push(setting);
  }

  return [...groups.entries()].map(([groupName, sections]) => [
    groupName,
    [...sections.entries()]
  ]);
}

export function countGroupSettings(sections) {
  return sections.reduce((count, [, settings]) => count + settings.length, 0);
}

export function labelize(value) {
  return value
    .replace(/([a-z0-9])([A-Z])/g, "$1 $2")
    .replace(/_/g, " ")
    .replace(/\b\w/g, (letter) => letter.toUpperCase());
}
