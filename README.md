# OpLogin
OpLogin is a simple and effective Minecraft plugin designed to enhance server security by requiring operators to authenticate with a password before they can execute commands. Perfect for server administrators who want to ensure that only authorized and authenticated operators can perform administrative tasks.

## Features
- Password Protection: Require OPs to log in with a password before they can use administrative commands.
- Password Management: Set and reset OP passwords easily via console commands.
- Persistent Storage: Passwords are saved across server restarts to ensure seamless operation.
- Console Security: Sensitive commands are hidden from the console log to protect against unauthorized access.
- Command Whitelist: Configure which commands can be used without requiring login.

## Commands
- `/opsetpass <player> <password>`
  Sets a password for the specified OP player. This command can only be executed from the console.
- `/oplogin <password>`
  Allows an OP player to log in with their password. Once logged in, the player can access all OP commands.
- `/resetop <player>`
  Resets the password for the specified OP player. This command can only be executed from the console.
- `/opreload`
  Reloads the plugin's data from disk. Note: Configuration changes require a server restart to take effect.

## Installation
1. Download the latest version of OpLogin.
2. Place the OpLogin.jar file into your server's plugins directory.
3. Start your server to generate default configuration files.
4. The plugin will create config.yml and whitelist.yml files in the plugins/OpLogin directory.

## Configuration
### config.yml
```yaml
# Security settings
security:
  # Minimum password length
  min-password-length: 8
  # Maximum failed login attempts before temporary block
  max-login-attempts: 3
  # Block duration in minutes after max failed attempts
  block-duration: 15

# Message settings (supports color codes)
messages:
  prefix: '&8[&bOpLogin&8] &7'
  login-success: '&aLogin successful! You can now use OP commands.'
  # ... other messages ...

# Stored encrypted passwords (DO NOT EDIT MANUALLY!)
passwords: {}
```
**Important:** Changes to config.yml require a server restart to take effect. The /opreload command only reloads stored passwords and whitelist entries.

### whitelist.yml
This file contains a list of commands that can be used without requiring login. By default, it includes:
```yaml
whitelisted-commands:
  - help
  - oplogin
  - list
  - ping
```
**Note:** Like config.yml, changes to whitelist.yml require a server restart to take effect.

## Usage
### Set a Password:
Use the `/opsetpass <player> <password>` command from the console to assign a password to an OP.

### Login:
OPs must use the `/oplogin <password>` command to authenticate themselves upon joining the server.

### Reset Password:
Use the `/resetop <player>` command from the console to remove an OP's current password. The OP will need to set a new password.

### Configuration Changes:
1. Stop your server
2. Edit the configuration files as needed
3. Start your server
4. Your changes will now be in effect

## Permissions
No specific permissions are required for basic operation. Commands are restricted to the console or OPs as described.

## Compatibility
- Minecraft Versions: Tested on Minecraft 1.20 and newer.
- Server Software: Works with Spigot, Paper, and other Bukkit-compatible server software.
