# 📦 Jenkins Pipeline Shared Library

This repository contains a **Shared Library** for [Jenkins Pipeline](https://www.jenkins.io/doc/book/pipeline/shared-libraries/), designed to centralize and reuse pipelines across multiple projects.

---

## 🚀 What is this?

This library allows you to define a collection of reusable pipelines and utilities for projects managed with Jenkins.

Its goals are to:  
- Keep CI/CD logic centralized.  
- Avoid duplicating `Jenkinsfile` logic across repositories.  
- Promote consistency and best practices between projects.

---

## 🛠 Requirements

- Jenkins with support for [Shared Libraries](https://www.jenkins.io/doc/book/pipeline/shared-libraries/).  
- Projects with a `Jenkinsfile` that invokes functions from this library.

---

## 📁 Repository structure

```
jenkins-pipeline-library/
├── vars/
│   └── <pipeline>.groovy      Global reusable pipeline functions
├── src/                       (optional) Groovy classes for complex logic
└── resources/                 (optional) Templates and static files
```

- **`vars/`**: Defines main functions that can be called directly from project `Jenkinsfile`s.  
- **`src/`**: Contains helper classes for more complex or reusable logic (optional).  
- **`resources/`**: Contains templates, YAML, or other static configurations used by pipelines (optional).

---

## ⚙️ Configure in Jenkins

1️⃣ Go to: **Manage Jenkins → Global Pipeline Libraries**  
2️⃣ Add a new library:
- **Name**: `ci-pipelines` (or any name you prefer)  
- **Default version**: `main` (or the branch you use)  
- **Retrieval method**: Modern SCM  
- **SCM**: Git  
- **Repository URL**: `https://github.com/your-username/jenkins-pipeline-library.git`

---

## 🧪 Usage in a project

In your project repository, use the library in your `Jenkinsfile` and invoke the desired pipeline function:

```groovy
@Library('ci-pipelines') _

myPipeline(
    param1: 'value1',
    param2: 'value2'
)
```

Each pipeline or function defines its own set of expected parameters.

---

## 💡 Best practices

✅ Keep this library under version control.  
✅ Document each function (using groovydoc) with its parameters and behavior.  
✅ Refactor when you identify repetitive patterns across projects.  
✅ Use `src/` for complex or hard-to-maintain logic outside `vars/`.

---

## 🤝 Contributing

Contributions are welcome! Please open an issue or submit a pull request to propose improvements or new features.

---

## 🧑‍💻 Maintainers

Developed and maintained by **Manuel Carbajal**.
