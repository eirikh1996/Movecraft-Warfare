# Movecraft-Warfare Addon

Home of the code for the following features:
 - Siege
 - Assault
 
 ## Download

Devevlopment builds can be found on the [GitHub Actions tab](https://github.com/APDevTeam/Movecraft-Warfare/actions) of this repository.  Stable builds can be found on [our SpigotMC page](https://www.spigotmc.org/resources/movecraft-warfare.87359/).

## Building
This plugin requires that the user setup and build their [Movecraft](https://github.com/APDevTeam/Movecraft) development environment, and then clone this into the same folder as your Movecraft development environment such that both Movecraft-Warfare and Movecraft are contained in the same folder.  This plugin also requires you to build the latest version of 1.13.2 using build tools.

```
java -jar BuildTools.jar --rev 1.13.2
```

Then, run the following to build Movecraft-Warfare through `maven`.
```
mvn clean install
```
Jars are located in `/target`.


## Support
[Github Issues](https://github.com/APDevTeam/Movecraft-Warfare/issues)

[Discord](http://bit.ly/JoinAP-Dev)

The plugin is released here under the GNU General Public License v3.
