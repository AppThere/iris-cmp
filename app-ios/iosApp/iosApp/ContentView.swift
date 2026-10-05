import IrisApp
import SwiftUI
import UIKit

/// Hosts the Compose UI from the IrisApp framework (built by Gradle in the "Compile Kotlin Framework" phase).
struct ContentView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.mainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}
