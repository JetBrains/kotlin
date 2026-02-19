import SwiftUI
import shared
import SpmLocalPackage

struct ContentView: View {
	let greet = Greeting().greet() + "\n" + greetingsFromSpmLocalPackage()

	var body: some View {
		Text(greet)
	}
}

struct ContentView_Previews: PreviewProvider {
	static var previews: some View {
		ContentView()
	}
}
