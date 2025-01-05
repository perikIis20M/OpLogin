# Changelog
All notable changes to OpLogin will be documented in this file.

## [1.1.0] - 2024-03-19
### Added
- Command whitelist system
  - New `whitelist.yml` configuration file
  - Ability to specify commands that don't require login
  - Default whitelisted commands: help, oplogin, list, ping
- New `/opreload` command
  - Console-only command to reload configuration
  - Reloads both passwords and whitelist without server restart
- Case-insensitive command handling
- Improved error messages with color formatting

### Changed
- Restructured configuration system
  - Split into separate config.yml and whitelist.yml
  - Better file handling and error checking
- Enhanced password management
  - Case-insensitive player name handling
  - Automatic saving on password changes
- Improved command security
  - Stricter console-only command enforcement
  - Better validation of command inputs

### Fixed
- Password persistence issues
- Command handling edge cases
- File creation and loading issues

## [1.0.0] - Initial Release
### Added
- Basic password protection for OP commands
- Console-only password management
- Three main commands:
  - `/opsetpass` - Set OP passwords
  - `/oplogin` - Authenticate as OP
  - `/resetop` - Reset OP passwords
- Persistent password storage
- Automatic logout on player disconnect 