# Changelog
All notable changes to OpLogin are documented in this file.

## [1.4.0]
### Added
- Locked operator session system that disables operator privileges until authentication.
- Manual `/oplogout` command.
- Emergency `/oplockdown` command.
- Sensitive command protection for high-risk administrative commands.
- Operator audit log for authenticated operator command usage.
- Temporary unlock timeout support.
- Optional auto-lock after sensitive commands.
- Optional PIN keypad login mode with randomized digit placement.
- Custom number-head keypad assets with credit to `minecraft-heads.com`.

### Changed
- Replaced reversible password storage with BCrypt hashing.
- Converted the default login flow to `/oplogin` plus private chat input instead of command arguments.
- Added one-time migration support for legacy AES-encrypted passwords.
- Updated trusted IP handling so manual logout clears remembered IP login state.
- Switched authentication mode changes to reset stored credentials and trusted IPs automatically.
- Corrected operator session cleanup so disconnecting while locked does not permanently remove operator status.
- Consolidated the plugin into the corrected 1.4.0 baseline.

### Fixed
- Disconnecting without logging in no longer strips operator from the account.
- Manual logout no longer allows auto-login on the next join from the remembered IP.
- PIN mode now enforces numeric-only credentials consistently.

## [1.3.0]
### Added
- IP-based auto-login system.
- Trusted IP storage and related configuration.

## [1.2.0]
### Added
- AES-encrypted password storage.
- Login attempt limiting and temporary blocking.
- Configurable security and message settings.

## [1.1.0]
### Added
- Command whitelist system.
- `/opreload` command.

## [1.0.0]
### Added
- Basic operator authentication.
- Console password management commands.
