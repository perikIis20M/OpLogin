# Changelog
All notable changes to OpLogin will be documented in this file.

## [1.2.0]
### Changed
- Updated Paper API dependency to 1.21.4
- Updated plugin version to reflect Minecraft version support
- Maintained compatibility with all 1.21.x versions

### Added
- Password encryption using AES
  - Automatic encryption key generation
  - Secure password storage
  - Encrypted password handling
- Enhanced security features
  - Password attempt limiting
  - Temporary blocking after failed attempts
  - Configurable security settings
- Configurable messages system
  - Customizable message prefix
  - Color code support
  - Placeholders for dynamic content
- New configuration options
  - Minimum password length
  - Maximum login attempts
  - Block duration settings
  - Customizable messages

### Changed
- Password storage system
  - Now uses encrypted format
  - Secure key storage
  - Better password validation
- Command handling
  - Hidden password input in console
  - Improved security checks
  - Better error handling
- Configuration structure
  - Added security settings section
  - Added messages section
  - More configurable options

### Security
- Implemented AES encryption for passwords
- Added brute force protection
- Improved password handling security
- Hidden password logging

## [1.1.0]
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

## [1.0.0]
### Added
- Basic password protection for OP commands
- Console-only password management
- Three main commands:
  - `/opsetpass` - Set OP passwords
  - `/oplogin` - Authenticate as OP
  - `/resetop` - Reset OP passwords
- Persistent password storage
- Automatic logout on player disconnect 