# Language Files

Customize messages in any language.

## Built-in Languages

- 🇺🇸 **English** (en)
- 🇸🇦 **Arabic** (ar)

## Change Language

```yaml
# config.yml
language: "en"  # or "ar"
```

Then: `/rt reload`

## Create Custom Language

1. Create `plugins/RelishTravel/lang/es.yml` (for Spanish)
2. Copy structure from `en.yml`
3. Translate all messages
4. Set `language: "es"` in config
5. Reload: `/rt reload`

## Message Format

Use [MiniMessage](https://docs.advntr.dev/minimessage/format.html):

```yaml
# Colors
"<red>Error"
"<green>Success"
"<#00d9ff>Custom hex"

# Gradients
"<gradient:#00d9ff:#00bfff>Text</gradient>"

# Formatting
"<bold>Bold</bold>"
"<italic>Italic</italic>"
```

## Placeholders

Keep these in your translations:
- `<charge>` - Charge percentage
- `<bar>` - Progress bar
- `<time>` - Time remaining
- `<height>` - Required height
- `<percent>` - Launch power

## Example Translation

```yaml
# lang/es.yml
prefix: "<gradient:#00d9ff:#00bfff>⚡ RelishTravel</gradient> <dark_gray>|</dark_gray> "

messages:
  charge:
    progress: "<gradient:#00d9ff:#00bfff>⚡ Cargando: <charge>%</gradient> <bar>"
    cancelled-moved: "<red>✖ <gray>¡Carga cancelada - te moviste!"
  
  launch:
    cooldown: "<#00d9ff>⌛ <gray>Espera <white><time></white> antes de lanzar"
```

## Share Your Translation

Created a translation? Share it on [Discord](https://discord.gg/jDr2KZcGXk) or [GitHub](https://github.com/iM5LB/relishtravel)!
