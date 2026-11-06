VortexExchange

<div align="center">VORTEXEXCHANGE

Advanced Auction House & Player Trading System

Developed by VortexTeam

""License" (https://img.shields.io/badge/License-Proprietary-red)" (LICENSE)
""Minecraft" (https://img.shields.io/badge/Minecraft-26.3-brightgreen)" (#requirements)
""Java" (https://img.shields.io/badge/Java-25-orange)" (#requirements)
""Discord" (https://img.shields.io/badge/Discord-Join%20Community-5865F2?logo=discord)" (https://discord.gg/CjvTjVVXK)

"Discord Community" (https://discord.gg/CjvTjVVXK)

</div>---

VortexExchange is a Minecraft server plugin that combines an advanced Auction House and Player Trading system.

It provides an integrated marketplace where players can list items for sale, trade items and in-game currency, and receive items through a persistent Mailbox system.

Designed for Paper-based Minecraft servers, VortexExchange focuses on convenient trading, persistent data storage, and transaction recovery.

---

Features

Category| Description
Auction House| Player-to-player marketplace for selling and listing items
Global Announcements| Broadcasts new auction listings to all online players
Listing Management| Configurable listing limits, minimum prices, and expiration
Auction Expiration| Automatically handles expired listings
Player Trading| Direct player-to-player item and money trading
Trade GUI| Inventory-based trading interface
Ready Confirmation| Both players must confirm before completing a trade
Trade Protection| Distance, world, cooldown, damage, and disconnect restrictions
Mailbox| Stores items awaiting delivery to players
Trade Recovery| Persistent records for recovering unfinished trades
SQLite Database| Persistent storage for plugin data
Multi-Language| Indonesian and English message support
Vault Integration| Connects trading and auction transactions to the server economy

---

Auction House

The Auction House allows players to sell items to other players.

Features:

- Create listings with a custom selling price.
- Global announcements when a new item is listed.
- Display seller name, item information, quantity, and price.
- Configurable maximum listings per player.
- Configurable minimum selling price.
- Configurable listing duration.
- Automatic listing expiration.
- Return expired items through the Mailbox.
- Configurable auction tax.

Example:

/ah sell 5000

This lists the item in the player's main hand for 5,000 in-game currency.

---

Player Trading

The Player Trading system allows two players to exchange items and in-game currency.

Features:

- Send trade requests to other players.
- Accept or deny incoming requests.
- GUI-based item exchange.
- Money trading through the economy system.
- Individual player ready confirmation.
- Trade completion after both players confirm.
- Configurable trading distance.
- Optional same-world restriction.
- Request expiration and cooldown.
- Optional cancellation when a player takes damage or disconnects.

Example:

/trade PlayerName
/trade accept
/trade deny

---

Mailbox System

The Mailbox system handles items that need to be delivered to players.

Features:

- Persistent item storage.
- Automatic delivery when players join.
- Supports expired auction item returns.
- Supports trade recovery deliveries.
- Stores undelivered items for later delivery.

---

Trade Recovery System

VortexExchange includes persistent trade records to help recover unfinished transactions.

Features:

- Saves active trade information.
- Stores both players' items and money.
- Checks unfinished trades after server interruptions.
- Returns stored items through the Mailbox.
- Uses database transactions during recovery.

---

Language Support

VortexExchange currently supports two languages:

Language| Configuration
Indonesian| "id"
English| "en"

Language selection is global and configured through "config.yml".

settings:
  language: "en"

Set the value to "id" to use Indonesian.

---

Commands

Command| Description
"/ah"| Open the Auction House
"/ah sell <price>"| Sell the item in your main hand
"/trade <player>"| Send a trade request
"/trade accept"| Accept a trade request
"/trade deny"| Deny a trade request
"/trade toggle"| Toggle incoming trade requests
"/trade money"| Trade money using the trade system

Command availability may depend on the installed plugin version.

---

Permissions

Permission| Default| Description
"vortexexchange.use"| Everyone| Access Auction House and Trading
"vortexexchange.admin"| OP| Administrative permission

---

Requirements

Requirement| Version| Required
Minecraft Server| 26.3| Yes
Paper| 26.3| Yes
Purpur| 26.3| Yes
Java| 25| Yes
Vault| Compatible version| Yes
Economy Plugin| Vault-compatible| Yes
SQLite JDBC| Bundled| Yes

Supported server software:

- Paper
- Purpur

VortexExchange targets the Paper 26.3 API. Compatibility with other server software or versions is not guaranteed.

---

Installation

1. Download the latest VortexExchange ".jar" release.
2. Install Vault on your server.
3. Install a Vault-compatible economy plugin.
4. Place "VortexExchange.jar" inside your server's "plugins/" directory.
5. Restart your server.
6. Open "plugins/VortexExchange/config.yml".
7. Adjust the settings to your preference.
8. Start using "/ah" and "/trade".

---

Configuration

The plugin generates its configuration files automatically.

Example "config.yml":

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

Configuration values can be adjusted to suit your server's economy and trading rules.

---

Database

VortexExchange uses SQLite for persistent data storage.

Database-backed systems include:

- Auction listings.
- Active trade records.
- Mailbox deliveries.
- Trade recovery information.

---

Support & Community

Need help with installation, configuration, or troubleshooting?

Join the official VortexTeam Discord community.

<div align="center">VortexTeam Community

"JOIN OUR DISCORD" (https://discord.gg/CjvTjVVXK)

https://discord.gg/CjvTjVVXK

</div>---

License

VortexExchange is distributed under the VortexExchange Proprietary License — All Rights Reserved.

Viewing this public repository does not grant permission to copy, modify, redistribute, reupload, or commercially exploit the software without prior written authorization from VortexTeam.

See "LICENSE" (LICENSE) for the complete terms.

---

<div align="center">VortexExchange

Built for Minecraft Trading.

Developed by VortexTeam

© 2026 VortexTeam. All Rights Reserved.

</div>
