
# VortexExchange

<p align="center">
  <img src="https://img.shields.io/badge/VortexTeam-Developer-00AEEF?style=for-the-badge" />
  <img src="https://img.shields.io/badge/Minecraft-26.3-brightgreen?style=for-the-badge" />
  <img src="https://img.shields.io/badge/Java-25-orange?style=for-the-badge" />
  <img src="https://img.shields.io/badge/License-Proprietary-red?style=for-the-badge" />
</p>

<p align="center">
  <strong>Advanced Auction House & Player Trading System</strong>
  <br />
  A complete trading solution for Minecraft servers.
  <br /><br />
  <a href="https://discord.gg/CjvTjVVXK">
    <img src="https://img.shields.io/badge/Join%20Our%20Discord-5865F2?style=for-the-badge&logo=discord&logoColor=white" />
  </a>
</p>

---

## Overview

**VortexExchange** is an advanced Minecraft plugin developed by **VortexTeam**.

It combines an Auction House, Player Trading, Mailbox, and Trade Recovery system into one integrated plugin.

Designed to provide a convenient and reliable trading experience for Minecraft server communities.

## Features

| Feature | Description |
|:--|:--|
| Auction House | Player-to-player item marketplace |
| Global Announcements | Announces new auction listings to online players |
| Listing Management | Configurable listing limits and minimum prices |
| Auction Expiration | Automatically handles expired listings |
| Player Trading | Exchange items and in-game currency |
| Trade GUI | Inventory-based trading interface |
| Ready Confirmation | Both players must confirm a transaction |
| Trade Protection | Distance, world, cooldown, damage and disconnect restrictions |
| Mailbox | Stores items awaiting delivery |
| Trade Recovery | Recovery support for unfinished trades |
| SQLite Database | Persistent plugin data storage |
| Vault Integration | Economy integration through Vault |
| Multi-Language | Indonesian and English support |

## Auction House

A player marketplace that allows users to sell their items.

**Features**
- Create item listings with custom prices.
- Broadcast new listings to all online players.
- Display seller, item, quantity and price.
- Configure maximum listings per player.
- Configure minimum selling prices.
- Configure listing expiration.
- Automatically handle expired listings.
- Return expired items through Mailbox.
- Configure auction tax.

**Command**
```text
/ah
/ah sell <price>
```

**Example**
```text
/ah sell 5000
```

Lists the item in your main hand for 5,000 in-game currency.

## Player Trading

A direct trading system between two players.

**Features**
- Send trade requests.
- Accept or deny requests.
- Exchange items through a GUI.
- Exchange in-game currency.
- Individual ready confirmation.
- Complete transactions after both players confirm.
- Configurable maximum trading distance.
- Optional same-world requirement.
- Request expiration and cooldown.
- Optional cancellation on damage or disconnect.

**Commands**
```text
/trade <player>
/trade accept
/trade deny
```

## Mailbox System

A persistent delivery system for player items.

**Features**
- Store items awaiting delivery.
- Automatically deliver stored items when players join.
- Handle expired auction item returns.
- Support trade recovery deliveries.
- Preserve undelivered items.

## Trade Recovery

Helps recover unfinished trades after server interruptions.

**Features**
- Persistent active trade records.
- Stores both players' items and money.
- Checks unfinished trades during recovery.
- Returns stored items through Mailbox.
- Uses database transactions during recovery.

## Commands

| Command | Description |
|:--|:--|
| `/ah` | Open Auction House |
| `/ah sell <price>` | List the item in your hand |
| `/trade <player>` | Send a trade request |
| `/trade accept` | Accept a trade request |
| `/trade deny` | Deny a trade request |

## Permissions

| Permission | Default | Description |
|:--|:--|:--|
| `vortexexchange.use` | Everyone | Access Auction House and Trading |
| `vortexexchange.admin` | OP | Administrative permission |

## Requirements

| Requirement | Version |
|:--|:--|
| Minecraft | 26.3 |
| Paper | 26.3 |
| Purpur | 26.3 |
| Java | 25 |
| Vault | Compatible version |
| Economy Plugin | Vault-compatible |

**Supported server software**
- Paper
- Purpur

VortexExchange targets the Paper 26.3 API. Other versions and server software have not been verified.

## Installation

1. Download the latest VortexExchange JAR release.
2. Install Vault.
3. Install a Vault-compatible economy plugin.
4. Upload `VortexExchange.jar` into your server's `plugins` folder.
5. Restart your server.
6. Open `plugins/VortexExchange/config.yml`.
7. Configure the plugin.
8. Start using `/ah` and `/trade`.

## Configuration

Configuration is generated automatically when the plugin starts for the first time.

**Example `config.yml`**

```yaml
settings:
  prefix: "&b&lVortexExchange &8»"
  language: "en"
  debug: false

auction:
  enabled: true
  max-listings-per-player: 10
  listing-duration-hours: 168
  minimum-price: 1
  tax-percent: 0
  expired-items-return: true

trade:
  enabled: true
  request-expire-seconds: 60
  max-distance: 15
  require-same-world: true
  cooldown-seconds: 5
  allow-money: true
  allow-items: true
  cancel-on-damage: true
  cancel-on-disconnect: true
```

## Language Support

VortexExchange supports two languages.

| Language | Value |
|:--|:--|
| Indonesian | `id` |
| English | `en` |

Change the language globally in `config.yml`.

```yaml
settings:
  language: "en"
```

## Database

VortexExchange uses SQLite for persistent storage.

Stored information includes:
- Auction listings.
- Active trade records.
- Mailbox deliveries.
- Trade recovery information.

## Support

Need help with installation, configuration or troubleshooting?

Join our official Discord community.

<p align="center">
  <a href="https://discord.gg/CjvTjVVXK">
    <img src="https://img.shields.io/badge/DISCORD-JOIN%20VORTEXTEAM-5865F2?style=for-the-badge&logo=discord&logoColor=white" />
  </a>
</p>

## License

VortexExchange is protected by the **VortexExchange Proprietary License**.

All rights are reserved by VortexTeam.

Unauthorized copying, modification, redistribution, reuploading or commercial use is prohibited without prior written permission.

See [LICENSE](LICENSE) for complete terms.

---

<p align="center">
  <strong>VortexExchange</strong>
  <br />
  Developed by <strong>VortexTeam</strong>
  <br />
  © 2026 VortexTeam. All Rights Reserved.
</p>
