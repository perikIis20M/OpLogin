# OpLogin
OpLogin is a Paper plugin that keeps operator accounts usable for normal gameplay while locking operator privileges behind authentication. Operators join in a locked state, unlock only when needed, and return to a protected state automatically or manually.

## Features
- Locked operator sessions that disable operator privileges until authentication.
- BCrypt password storage with one-time migration support for older AES-encrypted entries.
- Chat-safe password login flow: `/oplogin` starts login and the next chat message is captured privately.
- Optional PIN keypad mode with randomized digit positions and custom number heads.
- Manual `/oplogout` support that immediately locks privileges and clears trusted IP auto-login state.
- Sensitive command protection that overrides whitelist mistakes for high-risk commands.
- Temporary unlock timeouts and optional auto-locking after sensitive commands.
- Failed login attempt tracking with temporary blocking.
- Operator command audit logging.
- Emergency `/oplockdown` command to force all online operators back into the locked state.
- Trusted IP auto-login support that only applies after a successful authenticated login.

## Commands
- `/opsetpass <player> <password>`
  Console-only. Sets or replaces an operator credential.
- `/oplogin`
  Starts authentication. In password mode the next chat message is treated as the password. In PIN mode a keypad GUI opens.
- `/oplogout`
  Locks the current operator session immediately and clears remembered IP login for that operator.
- `/resetop <player>`
  Console-only. Removes an operator credential.
- `/opreload`
  Console-only. Reloads config and whitelist data.
- `/oplockdown`
  Console-only. Forces all online operators back into the locked state.

## Authentication Modes
### Password Mode
- Default mode.
- Operators use `/oplogin`, then type their password in chat.
- Passwords must meet `security.min-password-length`.

### PIN Mode
- Optional and disabled by default.
- Operators use `/oplogin` and receive a randomized keypad GUI.
- Stored credentials must be numeric and match `security.pin-mode.length`.
- Number head textures in the keypad use assets from [minecraft-heads.com](https://minecraft-heads.com/).

### Important Mode Change Behavior
- Switching between password mode and PIN mode resets all stored operator credentials and trusted IP entries on the next startup or `/opreload`.
- This is intentional so text passwords cannot remain active in PIN mode, and PIN-only credentials do not carry over into password mode.

## Installation
1. Download the latest jar.
2. Place it in the server `plugins` directory.
3. Start the server once to generate `config.yml` and `whitelist.yml`.
4. Configure the plugin and reload or restart the server.

## Configuration
### `config.yml`
Relevant options:

```yaml
security:
  min-password-length: 8
  max-login-attempts: 3
  block-duration: 15
  enable-ip-auto-login: true
  unlock-duration-seconds: 300
  auto-lock-ops-on-join: true
  pin-mode:
    enabled: false
    length: 4
    title: '&8OpLogin PIN'
```

Notes:
- `enable-ip-auto-login` remembers the IP only after a successful authenticated login.
- Using `/oplogout` clears the remembered IP for that operator immediately.
- If PIN mode is enabled, credentials set with `/opsetpass` must be numeric and match the configured PIN length.

### `whitelist.yml`
Commands listed here can still be used while locked, except for sensitive commands that are always blocked until authentication.

Default example:

```yaml
whitelisted-commands:
  - help
  - oplogin
  - list
  - ping
```

## Usage
### Set a Credential
Use `/opsetpass <player> <password>` from the console.

### Log In
Use `/oplogin`.

### Log Out
Use `/oplogout` to re-lock privileges without leaving the server.

### Reset a Credential
Use `/resetop <player>` from the console.

## Compatibility
- Minecraft: Paper 1.21.x
- Java: 21

## Credits
- PIN keypad head textures: [minecraft-heads.com](https://minecraft-heads.com/)
